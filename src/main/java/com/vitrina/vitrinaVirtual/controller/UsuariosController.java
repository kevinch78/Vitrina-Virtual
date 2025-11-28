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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios", description = "Gestión de perfiles de usuario")
@SecurityRequirement(name = "bearerAuth")
public class UsuariosController {
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT')")
    @Operation(summary = "Obtener perfil de usuario", description = "Obtiene el perfil del usuario autenticado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil obtenido exitosamente"),
        @ApiResponse(responseCode = "401", description = "Token inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo clientes")
    })
    public ResponseEntity<UserProfileDto> getUserProfile(HttpServletRequest request) {
        String token = jwtTokenProvider.getTokenFromRequest(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        String username = jwtTokenProvider.getUsernameFromToken(token);
        return ResponseEntity.ok(authService.getUserProfile(username));
    }
}