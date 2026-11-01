package com.memoryassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequest {

    @NotBlank(message = "message must not be blank")
    @Size(max = 4000, message = "message must not exceed 4000 characters")
    private String message;

    private Long sessionId;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
}
