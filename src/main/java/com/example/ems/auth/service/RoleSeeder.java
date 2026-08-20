package com.example.ems.auth.service;

import com.example.ems.auth.entity.Role;
import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures every {@link RoleName} has a corresponding row, independent of how the schema
 * was provisioned (Flyway in production, Hibernate ddl-auto in tests).
 */
// I added this after finding out our H2 test profile skips Flyway entirely (ddl-auto generates
// the tables but not the seed data from V2__create_roles.sql), so tests were failing with
// "no ROLE_EMPLOYEE row" before a single request even ran. Rather than special-case test
// config, I made role seeding self-healing on every boot - it's idempotent (existence check
// per role) so running it again in production against an already-migrated DB is a no-op.
// @Order(1) matters: AdminBootstrapRunner needs the ADMIN role to already exist, so this has
// to run first.
@Component
@RequiredArgsConstructor
@Order(1)
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
            }
        }
    }
}
