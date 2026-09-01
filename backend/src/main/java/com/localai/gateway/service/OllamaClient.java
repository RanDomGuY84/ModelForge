package com.localai.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Minimal client for the Ollama HTTP API (https://github.com/ollama/ollama/blob/main/docs/api.md)
 */
@Service
public class OllamaClient {

    private final RestTemplate restTemplate;

    @Value("${localai.ollama.base-url}")
    private String baseUrl;

    public OllamaClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public String chat(String model, List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(baseUrl + "/api/chat", entity, Map.class);

        if (response == null) {
            throw new IllegalStateException("No response from Ollama - is it running at " + baseUrl + "?");
        }
        Map<String, Object> message = (Map<String, Object>) response.get("message");
        if (message == null || message.get("content") == null) {
            throw new IllegalStateException("Unexpected Ollama response shape: " + response);
        }
        return (String) message.get("content");
    }

    @SuppressWarnings("unchecked")
    public List<String> listModels() {
        Map<String, Object> response = restTemplate.getForObject(baseUrl + "/api/tags", Map.class);
        List<String> names = new ArrayList<>();
        if (response == null) return names;
        List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");
        if (models == null) return names;
        for (Map<String, Object> m : models) {
            Object name = m.get("name");
            if (name != null) names.add(name.toString());
        }
        return names;
    }
}
