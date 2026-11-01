package com.memoryassistant.dto;

import com.memoryassistant.entity.FactCategory;
import com.memoryassistant.entity.MemoryFact;

import java.time.Instant;

public class MemoryFactDto {

    private Long id;
    private FactCategory category;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public static MemoryFactDto from(MemoryFact fact) {
        MemoryFactDto dto = new MemoryFactDto();
        dto.id = fact.getId();
        dto.category = fact.getCategory();
        dto.content = fact.getContent();
        dto.createdAt = fact.getCreatedAt();
        dto.updatedAt = fact.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public FactCategory getCategory() { return category; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
