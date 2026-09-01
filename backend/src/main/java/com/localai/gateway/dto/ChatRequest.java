package com.localai.gateway.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    /** Existing conversation to continue. Leave null to start a new one. */
    private String conversationId;

    /** Only used when starting a new conversation. */
    private String projectId;

    /** Overrides the project/default model for this call. */
    private String model;

    @NotBlank
    private String message;

    /** "user" | "tool" - lets the agent runtime feed tool results back into the loop. */
    private String role = "user";

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
