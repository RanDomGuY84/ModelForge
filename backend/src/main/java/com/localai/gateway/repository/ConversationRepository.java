package com.localai.gateway.repository;

import com.localai.gateway.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findAllByOrderByUpdatedAtDesc();
    List<Conversation> findByProjectIdOrderByUpdatedAtDesc(String projectId);
}
