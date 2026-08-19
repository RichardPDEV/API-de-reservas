package com.example.reservas.service;

import com.example.reservas.domain.ValidationException;
import com.example.reservas.domain.User;
import com.example.reservas.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.security.SecureRandom;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    @Autowired(required = false)
    private EmailService emailService;

    public UserService(UserRepository userRepo) { this.userRepo = userRepo; }

    public User register(String username, String password, String displayName) {
        return register(username, password, displayName, com.example.reservas.domain.UserRole.USER);
    }

    public User register(String username, String password, String displayName, com.example.reservas.domain.UserRole role) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase();
        String normalizedDisplayName = displayName == null ? "" : displayName.trim();
        if (normalizedUsername.isBlank() || password == null || password.isBlank() || normalizedDisplayName.isBlank()) {
            throw new ValidationException("campos_requeridos");
        }
        if (userRepo.findByUsername(normalizedUsername).isPresent()) throw new ValidationException("username ya existe");
        User u = new User();
        u.setUsername(normalizedUsername);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setDisplayName(normalizedDisplayName);
        u.setRole(role == null ? com.example.reservas.domain.UserRole.USER : role);
        // generate confirmation code and expiry
        String code = generateNumericCode(6);
        u.setConfirmationCode(code);
        u.setConfirmationExpiresAt(OffsetDateTime.now().plus(Duration.ofHours(1)));
        u.setEmailVerified(false);
        User saved = userRepo.save(u);
        if (emailService != null) {
            emailService.sendConfirmationCode(saved.getUsername(), code);
        }
        return saved;
    }

    public boolean confirmEmail(String username, String code) {
        var opt = userRepo.findByUsername(username);
        if (opt.isEmpty()) return false;
        User u = opt.get();
        if (u.getConfirmationCode() == null) return false;
        if (!u.getConfirmationCode().equals(code)) return false;
        if (u.getConfirmationExpiresAt() == null || u.getConfirmationExpiresAt().isBefore(OffsetDateTime.now())) return false;
        u.setEmailVerified(true);
        u.setConfirmationCode(null);
        u.setConfirmationExpiresAt(null);
        userRepo.save(u);
        return true;
    }

    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(secureRandom.nextInt(10));
        return sb.toString();
    }

    public boolean resendConfirmationCode(String username) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase();
        if (normalizedUsername.isBlank()) return false;
        var opt = userRepo.findByUsername(normalizedUsername);
        if (opt.isEmpty()) return false;
        User u = opt.get();
        String code = generateNumericCode(6);
        u.setConfirmationCode(code);
        u.setConfirmationExpiresAt(OffsetDateTime.now().plus(Duration.ofHours(1)));
        userRepo.save(u);
        if (emailService != null) {
            emailService.sendConfirmationCode(u.getUsername(), code);
        }
        return true;
    }

    public Optional<User> authenticate(String username, String password) {
        return userRepo.findByUsername(username)
            .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()) && u.isEmailVerified());
    }

    public void saveRefreshToken(Long userId, String refreshToken) {
        userRepo.findById(userId).ifPresent(u -> { u.setRefreshToken(refreshToken); userRepo.save(u); });
    }

    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }
}
