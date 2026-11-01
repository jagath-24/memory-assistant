package com.memoryassistant.controller;

import com.memoryassistant.dto.ConversationDto;
import com.memoryassistant.dto.CreateConversationRequest;
import com.memoryassistant.entity.ConversationSession;
import com.memoryassistant.entity.MessageRole;
import com.memoryassistant.repository.ConversationSessionRepository;
import com.memoryassistant.repository.MessageRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationSessionRepository sessionRepository;
    private final MessageRepository messageRepository;

    public ConversationController(ConversationSessionRepository sessionRepository,
                                   MessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping
    public List<ConversationDto> getAll() {
        return sessionRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(ConversationDto::from).toList();
    }

    @PostMapping
    public ConversationDto create(@RequestBody(required = false) CreateConversationRequest request) {
        ConversationSession session = new ConversationSession();
        String title = (request != null && request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle() : "New conversation";
        session.setTitle(title);
        return ConversationDto.from(sessionRepository.save(session));
    }

    @GetMapping("/{id}/messages")
    public List<Map<String, Object>> getMessages(@PathVariable Long id) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(id)
                .stream()
                .map(m -> Map.<String, Object>of(
                        "role", m.getRole().name().toLowerCase(),
                        "content", m.getContent()
                ))
                .toList();
    }

    /**
     * Back-fills titles for all sessions still named "New conversation"
     * using the first user message in each. Returns the updated list.
     */
    @PostMapping("/retitle")
    public List<ConversationDto> retitleAll() {
        List<ConversationSession> untitled = sessionRepository.findByTitle("New conversation");
        for (ConversationSession session : untitled) {
            messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                    .stream()
                    .filter(m -> m.getRole() == MessageRole.USER)
                    .findFirst()
                    .ifPresent(firstMessage -> {
                        String title = firstMessage.getContent().trim();
                        if (title.length() > 60) title = title.substring(0, 57) + "…";
                        session.setTitle(title);
                        sessionRepository.save(session);
                    });
        }
        return sessionRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(ConversationDto::from).toList();
    }
}
