package com.localai.gateway.repository;

import com.localai.gateway.model.ToolEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ToolEventRepository extends JpaRepository<ToolEvent, String> {
    List<ToolEvent> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    List<ToolEvent> findAllByOrderByCreatedAtDesc();
}
