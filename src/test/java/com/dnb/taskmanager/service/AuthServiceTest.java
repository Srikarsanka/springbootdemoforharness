package com.dnb.taskmanager.service;

import com.dnb.taskmanager.dto.request.LoginRequest;
import com.dnb.taskmanager.dto.request.RegisterRequest;
import com.dnb.taskmanager.dto.response.AuthResponse;
import com.dnb.taskmanager.dto.response.UserResponse;
import com.dnb.taskmanager.exception.DuplicateResourceException;
import com.dnb.taskmanager.model.User;
import com.dnb.taskmanager.model.enums.UserRole;
import com.dnb.taskmanager.repository.UserRepository;
import com.dnb.taskmanager.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserService userService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, tokenProvider, userService);
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void register_Success() {
        RegisterRequest request = new RegisterRequest(
                "john.doe@example.com", "John", "Doe", "password123", UserRole.DEVELOPER);

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id("user-123")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("encodedPassword")
                .role(UserRole.DEVELOPER)
                .createdAt(Instant.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken("john.doe@example.com")).thenReturn("jwt-mock-token");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);

        UserResponse userResponse = new UserResponse(
                "user-123", "john.doe@example.com", "John", "Doe", UserRole.DEVELOPER, savedUser.getCreatedAt());
        when(userService.toUserResponse(savedUser)).thenReturn(userResponse);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt-mock-token");
        assertThat(response.user().email()).isEqualTo("john.doe@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email is already taken")
    void register_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com", "Jane", "Doe", "password123", UserRole.DEVELOPER);

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User already exists with email");
    }

    @Test
    @DisplayName("Should successfully authenticate and return token upon login")
    void login_Success() {
        LoginRequest request = new LoginRequest("john.doe@example.com", "password123");

        Authentication auth = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("jwt-login-token");
        when(tokenProvider.getJwtExpirationMs()).thenReturn(86400000L);

        User user = User.builder()
                .id("user-123")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.DEVELOPER)
                .build();
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

        UserResponse userResponse = new UserResponse(
                "user-123", "john.doe@example.com", "John", "Doe", UserRole.DEVELOPER, Instant.now());
        when(userService.toUserResponse(user)).thenReturn(userResponse);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt-login-token");
        assertThat(response.user().id()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException when credentials invalid")
    void login_BadCredentials() {
        LoginRequest request = new LoginRequest("john.doe@example.com", "wrongpass");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}

