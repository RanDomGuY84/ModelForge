package com.localai.gateway.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tool_events")
public class ToolEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String conversationId;

    @Column(nullable = false)
    private String toolName;

    @Lob
    private String argsJson;

    /** "PENDING" | "ALLOWED" | "DENIED" | "SUCCEEDED" | "FAILED" */
    @Column(nullable = false)
    private String status = "PENDING";

    @Lob
    private String resultSummary;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getArgsJson() { return argsJson; }
    public void setArgsJson(String argsJson) { this.argsJson = argsJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
