package com.example.ems.common;

import com.example.ems.auth.entity.Role;
import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.auth.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TestSecurityContext {

    private TestSecurityContext() {
    }

    public static User userWithRoles(String email, RoleName... roleNames) {
        Set<Role> roles = Stream.of(roleNames)
                .map(name -> Role.builder().id(UUID.randomUUID()).name(name).build())
                .collect(Collectors.toSet());
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("irrelevant")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(roles)
                .build();
    }

    public static void authenticateAs(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
