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
import com.example.ems.auth.security.UserPrincipal;
import com.example.ems.common.exception.DuplicateResourceException;
import com.example.ems.common.exception.UnauthorizedException;
import com.example.ems.employee.entity.Employee;
import com.example.ems.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An account with email " + request.email() + " already exists");
        }

        // Self-registration always lands you as EMPLOYEE - there's no "pick your role" field
        // on the request on purpose, since letting a new signup choose ADMIN would be a pretty
        // obvious privilege-escalation hole. Getting anything higher requires an admin to grant
        // it out-of-band (see AdminBootstrapRunner for how the very first admin gets in).
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseThrow(() -> new IllegalStateException("Default role EMPLOYEE is not seeded in the database"));

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .enabled(true)
                .roles(Set.of(employeeRole))
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());
        return toUserResponse(saved);
    }

    public LoginResponse login(LoginRequest request) {
        // Delegating to Spring's AuthenticationManager instead of manually loading the user
        // and calling passwordEncoder.matches() myself - it already wires up the
        // UserDetailsService + BCrypt comparison correctly, and re-implementing that by hand
        // is exactly the kind of thing that's easy to get subtly wrong with auth code.
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        log.info("User logged in: {}", user.getEmail());
        return LoginResponse.of(accessToken, refreshToken, jwtService.getAccessTokenExpirationMs(), toUserResponse(user));
    }

    // Issuing a brand new refresh token here too (not just a new access token) is a deliberate
    // "rotate on use" choice - it caps how long a single refresh token stays valid in practice,
    // so if one ever leaked, the blast radius is smaller than "valid for the full 7-day window
    // no matter what."
    public LoginResponse refresh(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Provided token is not a valid refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!jwtService.isTokenValid(refreshToken, user.getEmail())) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        UserPrincipal principal = new UserPrincipal(user);
        String newAccessToken = jwtService.generateAccessToken(principal);
        String newRefreshToken = jwtService.generateRefreshToken(principal);

        return LoginResponse.of(newAccessToken, newRefreshToken, jwtService.getAccessTokenExpirationMs(), toUserResponse(user));
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
        return toUserResponse(user);
    }

    // The frontend needs its own employeeId to hit the "my attendance" / "my payroll"
    // endpoints (which are keyed by employeeId, not userId), and /auth/me is the one call
    // every page already makes on load - cheaper to fold this lookup in here than to add
    // a whole second endpoint just to answer "what's my employee record."
    private UserResponse toUserResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        UUID employeeId = employeeRepository.findByUser_Id(user.getId())
                .map(Employee::getId)
                .orElse(null);
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.isEnabled(), roleNames, employeeId);
    }
}
