package com.project.disc_mapper.service;

import com.project.disc_mapper.dto.entity.ResetTokens;
import com.project.disc_mapper.repo.TokenRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class TokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private TokenRepo tokenRepo;

    @Autowired
    private MailSenderService mss;


    public boolean hasToken(Long userId) {
        return tokenRepo.existsByUserId(userId);
    }

    public Optional<ResetTokens> getToken(Long userId) {
        return tokenRepo.findByUserId(userId);
    }

    public boolean deleteIfExpired(Long userId, int validitySeconds) {
        return tokenRepo.findByUserId(userId)
                .filter(e -> !mss.isValidToken(e.getCreatedAt(), validitySeconds))
                .map(e -> {
                    tokenRepo.delete(e);
                    return true;
                })
                .orElse(false);
    }

    public boolean existsValid(Long userId, int validitySeconds) {
        return tokenRepo.findByUserId(userId)
                .filter(e -> mss.isValidToken(e.getCreatedAt(), validitySeconds))
                .isPresent();
    }

    public ResetTokens getRecoveryByUserId(Long userId) {
        return tokenRepo.findByUserId(userId)
                .orElse(new ResetTokens());
    }

    public String generateRecoveryKey() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
