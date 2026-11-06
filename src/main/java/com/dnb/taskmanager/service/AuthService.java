package com.dnb.taskmanager.service;

import com.dnb.taskmanager.dto.request.LoginRequest;
import com.dnb.taskmanager.dto.request.RegisterRequest;
import com.dnb.taskmanager.dto.response.AuthResponse;
import com.dnb.taskmanager.dto.response.UserResponse;
import com.dnb.taskmanager.exception.DuplicateResourceException;
import com.dnb.taskmanager.exception.ResourceNotFoundException;
import com.dnb.taskmanager.model.User;
import com.dnb.taskmanager.model.enums.UserRole;
import com.dnb.taskmanager.repository.UserRepository;
import com.dnb.taskmanager.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User already exists with email: " + request.email());
        }

        UserRole role = request.role() != null ? request.role() : UserRole.DEVELOPER;

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getEmail());
        UserResponse userResponse = userService.toUserResponse(savedUser);

        return new AuthResponse(token, tokenProvider.getJwtExpirationMs(), userResponse);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase().trim(),
                        request.password()
                )
        );

        String token = tokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));
        UserResponse userResponse = userService.toUserResponse(user);

        return new AuthResponse(token, tokenProvider.getJwtExpirationMs(), userResponse);
    }
}

