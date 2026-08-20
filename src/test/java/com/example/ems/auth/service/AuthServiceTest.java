package com.example.ems.auth.service;

import com.example.ems.auth.dto.LoginRequest;
import com.example.ems.auth.dto.LoginResponse;
import com.example.ems.auth.dto.RegisterRequest;
import com.example.ems.auth.dto.UserResponse;
import com.example.ems.auth.entity.Role;
import com.example.ems.auth.entity.RoleName;
import com.example.ems.auth.entity.User;
import com.example.ems.auth.repository.RoleRepository;
import com.example.ems.auth.repository.UserRepository;
import com.example.ems.auth.security.JwtService;
import com.example.ems.common.exception.DuplicateResourceException;
import com.example.ems.common.exception.UnauthorizedException;
import com.example.ems.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Role employeeRole;

    @BeforeEach
    void setUp() {
        employeeRole = Role.builder().id(UUID.randomUUID()).name(RoleName.EMPLOYEE).build();
        lenient().when(employeeRepository.findByUser_Id(any())).thenReturn(Optional.empty());
    }

    @Test
    void register_createsUserWithEmployeeRole_whenEmailIsAvailable() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "Jane", "Doe");
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(roleRepository.findByName(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        UserResponse response = authService.register(request);

        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.roles()).containsExactly("EMPLOYEE");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsDuplicateResourceException_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123", "Jane", "Doe");
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokens_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashed")
                .firstName("Jane")
                .lastName("Doe")
                .enabled(true)
                .roles(Set.of(employeeRole))
                .build();

        when(userRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(3600000L);

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("user@example.com");
    }

    @Test
    void login_throwsUnauthorizedException_whenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        doThrow(new BadCredentialsException("bad creds")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_throwsUnauthorizedException_whenTokenIsNotARefreshToken() {
        when(jwtService.isRefreshToken(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("some-access-token"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
