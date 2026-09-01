package com.localai.gateway.controller;

import com.localai.gateway.model.Project;
import com.localai.gateway.repository.ProjectRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;

    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public List<Project> list() {
        return projectRepository.findAll();
    }

    @PostMapping
    public Project create(@Valid @RequestBody Project project) {
        project.setId(null);
        return projectRepository.save(project);
    }

    @PutMapping("/{id}")
    public Project update(@PathVariable String id, @RequestBody Project update) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown projectId: " + id));
        if (update.getName() != null) existing.setName(update.getName());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        if (update.getDefaultModel() != null) existing.setDefaultModel(update.getDefaultModel());
        if (update.getWorkspacePath() != null) existing.setWorkspacePath(update.getWorkspacePath());
        return projectRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        projectRepository.deleteById(id);
    }
}
