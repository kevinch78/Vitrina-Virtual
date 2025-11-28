package com.vitrina.vitrinaVirtual.infraestructura.crud_interface;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.vitrina.vitrinaVirtual.infraestructura.entity.BlacklistedToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BlacklistedTokenCrudRepository extends JpaRepository<BlacklistedToken, Long> {
    Optional<BlacklistedToken> findByToken(String token);
    @Modifying
    @Query("DELETE FROM BlacklistedToken bt WHERE bt.expirationDate <= ?1")
    int deleteByExpirationDateBefore(LocalDateTime now);
}