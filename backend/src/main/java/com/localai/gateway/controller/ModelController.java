package com.localai.gateway.controller;

import com.localai.gateway.service.OllamaClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final OllamaClient ollamaClient;

    public ModelController(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @GetMapping
    public Map<String, List<String>> list() {
        return Map.of("models", ollamaClient.listModels());
    }
}
