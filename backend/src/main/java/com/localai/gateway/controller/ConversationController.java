package com.localai.gateway.controller;

import com.localai.gateway.model.Conversation;
import com.localai.gateway.model.Message;
import com.localai.gateway.repository.ConversationRepository;
import com.localai.gateway.repository.MessageRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationController(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping
    public List<Conversation> list(@RequestParam(required = false) String projectId) {
        if (projectId != null) {
            return conversationRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        }
        return conversationRepository.findAllByOrderByUpdatedAtDesc();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable String id) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown conversationId: " + id));
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        return Map.of("conversation", conversation, "messages", messages);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        conversationRepository.deleteById(id);
    }
}
