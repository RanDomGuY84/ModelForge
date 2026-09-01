package com.localai.gateway.service;

import com.localai.gateway.dto.ChatRequest;
import com.localai.gateway.dto.ChatResponse;
import com.localai.gateway.model.Conversation;
import com.localai.gateway.model.Message;
import com.localai.gateway.repository.ConversationRepository;
import com.localai.gateway.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ModelRouterService modelRouterService;
    private final OllamaClient ollamaClient;

    public ConversationService(ConversationRepository conversationRepository,
                                MessageRepository messageRepository,
                                ModelRouterService modelRouterService,
                                OllamaClient ollamaClient) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.modelRouterService = modelRouterService;
        this.ollamaClient = ollamaClient;
    }

    public ChatResponse handleChat(ChatRequest request) {
        Conversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown conversationId: " + request.getConversationId()));
        } else {
            conversation = new Conversation();
            conversation.setProjectId(request.getProjectId());
            conversation.setModel(modelRouterService.resolveModel(request.getModel(), request.getProjectId()));
            conversation.setTitle(deriveTitle(request.getMessage()));
            conversation = conversationRepository.save(conversation);
        }

        // Allow a per-call model override even on an existing conversation
        String model = request.getModel() != null && !request.getModel().isBlank()
                ? request.getModel()
                : conversation.getModel();

        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole(request.getRole() == null ? "user" : request.getRole());
        userMessage.setContent(request.getMessage());
        messageRepository.save(userMessage);

        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<Map<String, String>> ollamaMessages = new ArrayList<>();
        for (Message m : history) {
            ollamaMessages.add(Map.of("role", normalizeRole(m.getRole()), "content", m.getContent()));
        }

        String reply = ollamaClient.chat(model, ollamaMessages);

        Message assistantMessage = new Message();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply);
        messageRepository.save(assistantMessage);

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        return new ChatResponse(conversation.getId(), model, reply);
    }

    private String normalizeRole(String role) {
        // Ollama's chat API only understands system/user/assistant - fold "tool" results into user turns
        if ("tool".equals(role)) return "user";
        return role;
    }

    private String deriveTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) return "New conversation";
        String trimmed = firstMessage.strip();
        return trimmed.length() > 60 ? trimmed.substring(0, 60) + "…" : trimmed;
    }
}
