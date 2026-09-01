package com.localai.gateway.dto;

public class ChatResponse {
    private String conversationId;
    private String model;
    private String reply;

    public ChatResponse() {}

    public ChatResponse(String conversationId, String model, String reply) {
        this.conversationId = conversationId;
        this.model = model;
        this.reply = reply;
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
}
