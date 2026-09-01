package com.localai.gateway.service;

import com.localai.gateway.model.Permission;
import com.localai.gateway.repository.PermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Minimal permission engine: looks up rules for a tool, preferring a project-scoped
 * rule over a global one. Defaults to "ASK" (require human approval) when no rule exists,
 * which is the safe default for anything that touches the filesystem or runs commands.
 */
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public String checkDecision(String toolName, String projectId) {
        if (projectId != null) {
            List<Permission> scoped = permissionRepository.findByToolNameAndProjectId(toolName, projectId);
            if (!scoped.isEmpty()) {
                return scoped.get(0).getDecision();
            }
        }
        List<Permission> global = permissionRepository.findByToolNameAndProjectIdIsNull(toolName);
        if (!global.isEmpty()) {
            return global.get(0).getDecision();
        }
        return "ASK";
    }
}
