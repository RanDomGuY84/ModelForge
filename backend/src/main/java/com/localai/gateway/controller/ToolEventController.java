package com.localai.gateway.controller;

import com.localai.gateway.model.ToolEvent;
import com.localai.gateway.repository.ToolEventRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lets the agent runtime report what it did (or attempted) so the dashboard's
 * "Agent Activity" view has something to show, and so tool usage is auditable.
 */
@RestController
@RequestMapping("/api/tool-events")
public class ToolEventController {

    private final ToolEventRepository toolEventRepository;

    public ToolEventController(ToolEventRepository toolEventRepository) {
        this.toolEventRepository = toolEventRepository;
    }

    @GetMapping
    public List<ToolEvent> list(@RequestParam(required = false) String conversationId) {
        if (conversationId != null) {
            return toolEventRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        }
        return toolEventRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public ToolEvent create(@Valid @RequestBody ToolEvent event) {
        event.setId(null);
        return toolEventRepository.save(event);
    }

    @PutMapping("/{id}")
    public ToolEvent update(@PathVariable String id, @RequestBody ToolEvent update) {
        ToolEvent existing = toolEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown tool event id: " + id));
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        if (update.getResultSummary() != null) existing.setResultSummary(update.getResultSummary());
        return toolEventRepository.save(existing);
    }
}
