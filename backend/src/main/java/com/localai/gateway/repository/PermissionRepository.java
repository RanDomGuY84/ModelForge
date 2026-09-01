package com.localai.gateway.repository;

import com.localai.gateway.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, String> {
    List<Permission> findByProjectIdOrProjectIdIsNull(String projectId);
    List<Permission> findByToolNameAndProjectIdIsNull(String toolName);
    List<Permission> findByToolNameAndProjectId(String toolName, String projectId);
}
