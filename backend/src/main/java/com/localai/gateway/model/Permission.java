package com.localai.gateway.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Nullable: null = applies globally across all projects */
    private String projectId;

    /** Tool name this rule governs, e.g. "read_file", "write_file", "run_command" */
    @Column(nullable = false)
    private String toolName;

    /** "ALLOW" | "DENY" | "ASK" */
    @Column(nullable = false)
    private String decision = "ASK";

    /** Optional glob/path scope this rule applies to, e.g. "src/**" */
    private String pathScope;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getPathScope() { return pathScope; }
    public void setPathScope(String pathScope) { this.pathScope = pathScope; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
