package com.vitrina.vitrinaVirtual.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vitrina.vitrinaVirtual.domain.dto.UserProfileDto;
import com.vitrina.vitrinaVirtual.domain.service.AuthService;
import com.vitrina.vitrinaVirtual.infraestructura.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
public class UsuariosController {
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<UserProfileDto> getUserProfile(HttpServletRequest request) {
        String token = jwtTokenProvider.getTokenFromRequest(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        String username = jwtTokenProvider.getUsernameFromToken(token);
        return ResponseEntity.ok(authService.getUserProfile(username));
    }
}