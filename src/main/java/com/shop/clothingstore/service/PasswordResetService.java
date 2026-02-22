package com.shop.clothingstore.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.PasswordResetToken;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.PasswordResetTokenRepository;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public PasswordResetToken createTokenForUser(User user) {

        Optional<PasswordResetToken> existingToken =
                tokenRepository.findByUser(user);

        PasswordResetToken token;

        if (existingToken.isPresent()) {
            token = existingToken.get();
        } else {
            token = new PasswordResetToken();
            token.setUser(user);
        }

        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(LocalDateTime.now().plusMinutes(30));

        return tokenRepository.save(token);
    }
}