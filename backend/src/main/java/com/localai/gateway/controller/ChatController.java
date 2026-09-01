package com.localai.gateway.controller;

import com.localai.gateway.dto.ChatRequest;
import com.localai.gateway.dto.ChatResponse;
import com.localai.gateway.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ConversationService conversationService;

    public ChatController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return conversationService.handleChat(request);
    }
}
