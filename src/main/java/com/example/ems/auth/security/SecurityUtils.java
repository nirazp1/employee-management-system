package com.example.ems.auth.security;

import com.example.ems.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

// Small static helper so every service doesn't have to repeat the same
// SecurityContextHolder-plumbing dance to figure out "who's calling right now" - it's used
// everywhere from ownership checks to role gating, so I wanted one obviously-correct place
// for it instead of copy-pasted context lookups scattered across the services.
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("No authenticated user found in security context");
        }
        return principal;
    }

    public static UUID currentUserId() {
        return currentPrincipal().getId();
    }

    public static String currentUserEmail() {
        return currentPrincipal().getUsername();
    }

    public static boolean hasRole(String role) {
        return currentPrincipal().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
