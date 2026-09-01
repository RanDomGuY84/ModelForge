package com.localai.gateway.service;

import com.localai.gateway.model.Project;
import com.localai.gateway.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Decides which model to use for a given request.
 * Minimal precedence: explicit request model > project default model > global default model.
 * This is the natural place to later add rules like "route long context to model X"
 * or "route code tasks to a code-specialized model".
 */
@Service
public class ModelRouterService {

    private final ProjectRepository projectRepository;

    @Value("${localai.ollama.default-model}")
    private String globalDefaultModel;

    public ModelRouterService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public String resolveModel(String requestedModel, String projectId) {
        if (requestedModel != null && !requestedModel.isBlank()) {
            return requestedModel;
        }
        if (projectId != null) {
            Optional<Project> project = projectRepository.findById(projectId);
            if (project.isPresent() && project.get().getDefaultModel() != null) {
                return project.get().getDefaultModel();
            }
        }
        return globalDefaultModel;
    }
}
