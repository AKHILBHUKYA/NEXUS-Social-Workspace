package com.akhil.social.service;

import com.akhil.social.dto.AuthDtos.*;
import com.akhil.social.entity.User;
import com.akhil.social.exception.ApiException;
import com.akhil.social.repository.UserRepository;
import com.akhil.social.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username()))
            throw new ApiException("Username already taken", HttpStatus.CONFLICT);
        if (userRepository.existsByEmail(req.email()))
            throw new ApiException("Email already registered", HttpStatus.CONFLICT);
        User u = new User();
        u.setUsername(req.username().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setDisplayName(req.displayName() != null && !req.displayName().isBlank() ? req.displayName() : req.username());
        u.setRole("USER");
        userRepository.save(u);
        String token = jwtService.generate(u.getId(), u.getUsername(), u.getRole());
        return new AuthResponse(token, u.getId(), u.getUsername(), u.getDisplayName(), u.getRole());
    }

    public AuthResponse login(LoginRequest req) {
        User u = userRepository.findByUsername(req.usernameOrEmail())
                .or(() -> userRepository.findByEmail(req.usernameOrEmail().toLowerCase()))
                .orElseThrow(() -> new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash()))
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        String token = jwtService.generate(u.getId(), u.getUsername(), u.getRole());
        return new AuthResponse(token, u.getId(), u.getUsername(), u.getDisplayName(), u.getRole());
    }

    public UserResponse me(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.getAvatarUrl(), user.getBio(), user.getRole());
    }
}
