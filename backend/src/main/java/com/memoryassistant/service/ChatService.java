package com.memoryassistant.service;

import com.memoryassistant.dto.ChatResponse;
import com.memoryassistant.entity.ConversationSession;
import com.memoryassistant.entity.Message;
import com.memoryassistant.entity.MessageRole;
import com.memoryassistant.repository.ConversationSessionRepository;
import com.memoryassistant.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 20;

    private final GroqService groqService;
    private final MemoryService memoryService;
    private final MessageRepository messageRepository;
    private final ConversationSessionRepository sessionRepository;

    @Value("${groq.chat-model}")
    private String chatModel;

    public ChatService(GroqService groqService,
                        MemoryService memoryService,
                        MessageRepository messageRepository,
                        ConversationSessionRepository sessionRepository) {
        this.groqService = groqService;
        this.memoryService = memoryService;
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
    }

    public ChatResponse handleChat(String userMessage, Long sessionId) {
        Long resolvedSessionId = sessionId != null ? sessionId : createDefaultSession();

        List<Message> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(resolvedSessionId);

        List<Map<String, String>> prompt = new ArrayList<>();
        prompt.add(buildSystemMessage());

        int start = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        for (Message m : history.subList(start, history.size())) {
            prompt.add(toGroqMessage(m));
        }
        prompt.add(Map.of("role", "user", "content", userMessage));

        String reply = groqService.chatCompletion(prompt, chatModel, 0.7);

        saveMessage(resolvedSessionId, MessageRole.USER, userMessage);
        saveMessage(resolvedSessionId, MessageRole.ASSISTANT, reply);

        // Auto-title: on the very first message, use the message text as the session title
        String sessionTitle = maybeUpdateTitle(resolvedSessionId, userMessage, history.isEmpty());

        // Fire-and-forget — does not block the response
        memoryService.extractAndStore(userMessage, reply);

        return new ChatResponse(reply, resolvedSessionId, sessionTitle);
    }

    /**
     * If this is the first message in the session, update the title from "New conversation"
     * to a truncated version of the user's first message.
     */
    private String maybeUpdateTitle(Long sessionId, String userMessage, boolean isFirstMessage) {
        if (!isFirstMessage) {
            return sessionRepository.findById(sessionId)
                    .map(ConversationSession::getTitle)
                    .orElse(null);
        }
        String title = userMessage.trim();
        if (title.length() > 60) {
            title = title.substring(0, 57) + "…";
        }
        String finalTitle = title;
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setTitle(finalTitle);
            sessionRepository.save(session);
        });
        return finalTitle;
    }

    private Map<String, String> buildSystemMessage() {
        String memoryContext = memoryService.buildContextBlock();
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful, warm AI assistant with long-term memory about the user.\n");
        if (!memoryContext.isBlank()) {
            sb.append("\n").append(memoryContext);
        }
        Map<String, String> msg = new LinkedHashMap<>();
        msg.put("role", "system");
        msg.put("content", sb.toString());
        return msg;
    }

    private Map<String, String> toGroqMessage(Message m) {
        String role = m.getRole() == MessageRole.USER ? "user" : "assistant";
        return Map.of("role", role, "content", m.getContent());
    }

    private void saveMessage(Long sessionId, MessageRole role, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        messageRepository.save(message);
    }

    private Long createDefaultSession() {
        ConversationSession session = new ConversationSession();
        session.setTitle("New conversation");
        return sessionRepository.save(session).getId();
    }
}
