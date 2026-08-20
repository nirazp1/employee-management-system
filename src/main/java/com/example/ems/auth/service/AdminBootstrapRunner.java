package com.example.ems.auth.service;

import com.example.ems.auth.entity.Role;
import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.auth.repository.RoleRepository;
import com.example.ems.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Seeds a single ADMIN account on startup when ADMIN_EMAIL / ADMIN_PASSWORD are supplied
 * and no such account exists yet, so the system has at least one administrator without
 * requiring direct database access.
 */
@Component
@RequiredArgsConstructor
@Order(2)
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap-email:}")
    private String adminEmail;

    @Value("${app.admin.bootstrap-password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // This solves a chicken-and-egg problem: /auth/register only ever hands out EMPLOYEE,
        // and there's no "promote to admin" endpoint (on purpose - see the register() comment
        // in AuthService). Without this, the only way to get the very first admin into a fresh
        // deployment would be a manual SQL UPDATE. Both env vars being blank is the expected
        // steady-state after the first boot (or if the operator just never set them), so I
        // treat that as "nothing to do" rather than an error.
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role is not seeded in the database"));

        User admin = User.builder()
                .email(adminEmail.toLowerCase())
                .passwordHash(passwordEncoder.encode(adminPassword))
                .firstName("System")
                .lastName("Administrator")
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
        log.info("Bootstrapped initial ADMIN account: {}", admin.getEmail());
    }
}
