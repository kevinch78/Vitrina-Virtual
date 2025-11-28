package com.vitrina.vitrinaVirtual.domain.service;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vitrina.vitrinaVirtual.infraestructura.crud_interface.BlacklistedTokenCrudRepository;
import com.vitrina.vitrinaVirtual.infraestructura.entity.BlacklistedToken;

import java.time.LocalDateTime;

@Service
public class BlacklistService {

    @Autowired
    private BlacklistedTokenCrudRepository blacklistedTokenCrudRepository;

    @Transactional
    public void blacklistToken(String token, LocalDateTime expirationDate) {
        // Solo añadir si no está ya en la lista negra (evitar duplicados)
        if (blacklistedTokenCrudRepository.findByToken(token).isEmpty()) {
            BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                    .token(token)
                    .expirationDate(expirationDate)
                    .build();
            blacklistedTokenCrudRepository.save(blacklistedToken);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenCrudRepository.findByToken(token).isPresent();
    }

    @Transactional
    public void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = blacklistedTokenCrudRepository.deleteByExpirationDateBefore(now);
        if (deletedCount > 0) {
            System.out.println("Limpiados " + deletedCount + " tokens expirados de la lista negra.");
        }
    }
}