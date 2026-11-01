package com.memoryassistant.dto;

import com.memoryassistant.entity.ConversationSession;

import java.time.Instant;

public class ConversationDto {

    private Long id;
    private String title;
    private Instant createdAt;

    public static ConversationDto from(ConversationSession session) {
        ConversationDto dto = new ConversationDto();
        dto.id = session.getId();
        dto.title = session.getTitle();
        dto.createdAt = session.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
}
