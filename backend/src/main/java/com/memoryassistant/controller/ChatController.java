package com.memoryassistant.controller;

import com.memoryassistant.dto.ChatRequest;
import com.memoryassistant.dto.ChatResponse;
import com.memoryassistant.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.handleChat(request.getMessage(), request.getSessionId());
    }
}
