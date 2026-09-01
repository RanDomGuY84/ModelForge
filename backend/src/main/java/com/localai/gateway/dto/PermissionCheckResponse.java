package com.localai.gateway.dto;

public class PermissionCheckResponse {
    private String toolName;
    private String decision; // ALLOW | DENY | ASK

    public PermissionCheckResponse() {}

    public PermissionCheckResponse(String toolName, String decision) {
        this.toolName = toolName;
        this.decision = decision;
    }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}
