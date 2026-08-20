package com.example.ems.auth.repository;

import com.example.ems.auth.entity.Role;
import com.example.ems.auth.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}
