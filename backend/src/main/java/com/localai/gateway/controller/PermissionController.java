package com.localai.gateway.controller;

import com.localai.gateway.dto.PermissionCheckResponse;
import com.localai.gateway.model.Permission;
import com.localai.gateway.repository.PermissionRepository;
import com.localai.gateway.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionRepository permissionRepository;
    private final PermissionService permissionService;

    public PermissionController(PermissionRepository permissionRepository, PermissionService permissionService) {
        this.permissionRepository = permissionRepository;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<Permission> list() {
        return permissionRepository.findAll();
    }

    @PostMapping
    public Permission create(@Valid @RequestBody Permission permission) {
        permission.setId(null);
        return permissionRepository.save(permission);
    }

    @PutMapping("/{id}")
    public Permission update(@PathVariable String id, @RequestBody Permission update) {
        Permission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown permission id: " + id));
        if (update.getDecision() != null) existing.setDecision(update.getDecision());
        if (update.getPathScope() != null) existing.setPathScope(update.getPathScope());
        return permissionRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        permissionRepository.deleteById(id);
    }

    /** Used by the agent runtime before executing a tool call. */
    @GetMapping("/check")
    public PermissionCheckResponse check(@RequestParam String toolName,
                                          @RequestParam(required = false) String projectId) {
        String decision = permissionService.checkDecision(toolName, projectId);
        return new PermissionCheckResponse(toolName, decision);
    }
}
