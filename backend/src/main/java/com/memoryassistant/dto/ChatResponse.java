package com.memoryassistant.dto;

public class ChatResponse {

    private String reply;
    private Long sessionId;
    private String sessionTitle;

    public ChatResponse(String reply, Long sessionId, String sessionTitle) {
        this.reply = reply;
        this.sessionId = sessionId;
        this.sessionTitle = sessionTitle;
    }

    public String getReply() { return reply; }
    public Long getSessionId() { return sessionId; }
    public String getSessionTitle() { return sessionTitle; }
}
