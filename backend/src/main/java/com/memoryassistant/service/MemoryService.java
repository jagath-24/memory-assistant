package com.memoryassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoryassistant.dto.MemoryFactDto;
import com.memoryassistant.entity.FactCategory;
import com.memoryassistant.entity.MemoryFact;
import com.memoryassistant.repository.MemoryFactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);
    private static final int MAX_FACTS_IN_CONTEXT = 40;

    private final GroqService groqService;
    private final MemoryFactRepository factRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${groq.extraction-model}")
    private String extractionModel;

    public MemoryService(GroqService groqService, MemoryFactRepository factRepository) {
        this.groqService = groqService;
        this.factRepository = factRepository;
    }

    private static final String EXTRACTION_SYSTEM_PROMPT = """
            You extract durable, reusable facts about the user from a single chat exchange.
            Only extract facts that would still be true and useful weeks later - preferences,
            goals, stable personal/professional facts, or ongoing projects. Do NOT extract
            one-off requests, greetings, or transient chit-chat.

            Respond with ONLY a JSON array, no markdown, no preamble. Each element:
            {"category": "PREFERENCE" | "GOAL" | "FACT" | "PROJECT", "content": "short factual statement"}

            If there is nothing durable to extract, respond with an empty array: []
            """;

    /**
     * Runs a second Groq call (async, fire-and-forget) to pull durable facts out of the
     * latest exchange. Does not block the chat response.
     */
    @Async
    public void extractAndStore(String userMessage, String assistantReply) {
        String exchange = "User: " + userMessage + "\nAssistant: " + assistantReply;

        String raw;
        try {
            raw = groqService.chatCompletion(
                    List.of(
                            Map.of("role", "system", "content", EXTRACTION_SYSTEM_PROMPT),
                            Map.of("role", "user", "content", exchange)
                    ),
                    extractionModel,
                    0.0
            );
        } catch (Exception e) {
            log.warn("Fact extraction call failed, skipping this turn: {}", e.getMessage());
            return;
        }

        try {
            String cleaned = stripCodeFences(raw);
            JsonNode arr = mapper.readTree(cleaned);
            if (!arr.isArray()) return;

            for (JsonNode node : arr) {
                String categoryStr = node.path("category").asText("FACT");
                String content = node.path("content").asText("").trim();
                if (content.isEmpty()) continue;

                FactCategory category;
                try {
                    category = FactCategory.valueOf(categoryStr.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    category = FactCategory.FACT;
                }

                upsertFact(category, content);
            }
        } catch (Exception e) {
            log.warn("Could not parse extracted facts JSON, skipping: {}", e.getMessage());
        }
    }

    /**
     * Dedupe strategy for v1: exact case-insensitive match on (category, content) updates
     * the timestamp instead of inserting a duplicate row. Fuzzy/semantic dedupe is a v2 item.
     */
    private MemoryFact upsertFact(FactCategory category, String content) {
        return factRepository.findFirstByCategoryAndContentIgnoreCase(category, content)
                .map(existing -> {
                    existing.setUpdatedAt(java.time.Instant.now());
                    return factRepository.save(existing);
                })
                .orElseGet(() -> {
                    MemoryFact fact = new MemoryFact();
                    fact.setCategory(category);
                    fact.setContent(content);
                    return factRepository.save(fact);
                });
    }

    public List<MemoryFactDto> getAllFacts() {
        return factRepository.findAllByOrderByUpdatedAtDesc()
                .stream().map(MemoryFactDto::from).toList();
    }

    public void deleteFact(Long id) {
        factRepository.deleteById(id);
    }

    /**
     * Builds the memory block injected into the system prompt for every new message.
     * Capped so the prompt doesn't grow unbounded as facts accumulate.
     */
    public String buildContextBlock() {
        List<MemoryFact> facts = factRepository.findAllByOrderByUpdatedAtDesc();
        if (facts.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Known facts about the user (use naturally, don't just list them back):\n");
        facts.stream().limit(MAX_FACTS_IN_CONTEXT).forEach(f ->
                sb.append("- [").append(f.getCategory()).append("] ").append(f.getContent()).append("\n")
        );
        return sb.toString();
    }

    private String stripCodeFences(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(json)?", "").trim();
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3).trim();
            }
        }
        return s;
    }
}
