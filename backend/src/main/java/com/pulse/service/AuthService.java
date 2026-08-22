package com.pulse.service;

import com.pulse.dto.AuthResponse;
import com.pulse.dto.LoginRequest;
import com.pulse.dto.RegisterRequest;
import com.pulse.dto.ForgotPasswordRequest;
import com.pulse.dto.ResetPasswordRequest;
import com.pulse.entity.PasswordResetToken;
import com.pulse.entity.User;
import com.pulse.exception.EmailAlreadyExistsException;
import com.pulse.exception.InvalidCredentialsException;
import com.pulse.repository.PasswordResetTokenRepository;
import com.pulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        
        userRepository.save(user);
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("No account registered with email: " + request.getEmail()));

        // Invalidate previous unused tokens for this user
        List<PasswordResetToken> oldTokens = passwordResetTokenRepository.findByUserAndUsedFalse(user);
        for (PasswordResetToken oldToken : oldTokens) {
            oldToken.setUsed(true);
        }
        passwordResetTokenRepository.saveAll(oldTokens);

        // Generate 6-digit secure code
        SecureRandom random = new SecureRandom();
        String code = String.format("%06d", random.nextInt(1000000));

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(code)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), code);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or reset code"));

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUserAndUsedFalse(request.getCode(), user)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired reset code"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);
            throw new InvalidCredentialsException("Reset code has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
