package com.vitrina.vitrinaVirtual.infraestructura.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_tokens") // Es una buena práctica nombrar la tabla explícitamente
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512) // Los tokens JWT pueden ser largos
    private String token;

    @Column(nullable = false, name = "expiration_date")
    private LocalDateTime expirationDate;
}