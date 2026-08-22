package com.pulse.repository;

import com.pulse.entity.PasswordResetToken;
import com.pulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenAndUserAndUsedFalse(String token, User user);
    List<PasswordResetToken> findByUserAndUsedFalse(User user);
}
