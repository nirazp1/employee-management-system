package com.example.ems.auth.security;

import com.example.ems.auth.entity.Role;
import com.example.ems.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final Set<GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.enabled = user.isEnabled();
        // The "ROLE_" prefix isn't decoration - it's what Spring Security's hasRole('ADMIN')
        // expects under the hood (it silently prepends ROLE_ itself when you use hasRole, but
        // our own SecurityUtils.hasRole() and the @PreAuthorize hasAnyRole(...) checks all
        // assume this prefix already exists on the authority string).
        this.authorities = user.getRoles().stream()
                .map(Role::getName)
                .map(name -> new SimpleGrantedAuthority("ROLE_" + name.name()))
                .collect(Collectors.toSet());
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
