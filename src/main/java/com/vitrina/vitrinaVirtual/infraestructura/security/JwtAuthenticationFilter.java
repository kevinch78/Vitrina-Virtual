package com.vitrina.vitrinaVirtual.infraestructura.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        // Excluir endpoints públicos (auth y documentación) del filtro JWT
        String requestPath = request.getRequestURI();
        if (isPublicEndpoint(requestPath)) {
            logger.debug("Skipping JWT filter for documentation endpoint: {}", requestPath);
            chain.doFilter(request, response);
            return;
        }
        
        String header = request.getHeader("Authorization");
        String token = null;
        String username = null;
        String role = null;

        logger.debug("Authorization header: {}", header);

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
            try {
                logger.debug("Processing token: {}", token);
                username = jwtTokenProvider.getUsernameFromToken(token);
                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtTokenProvider.getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
                role = (String) claims.get("role");
                logger.debug("Extracted username: {}, role: {}", username, role);
            } catch (Exception e) {
                logger.warn("Token inválido o expirado: {}", e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtTokenProvider.validateToken(token)) {
                logger.debug("UserDetails loaded for {}, authorities: {}", username, userDetails.getAuthorities());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, Collections.singletonList(new SimpleGrantedAuthority(role)));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authentication set for user: {}", username);
            } else {
                logger.warn("Token validation failed for username: {}", username);
            }
        } else {
            logger.debug("No username or authentication already set");
        }

        chain.doFilter(request, response);
    }
    
    /**
     * Verifica si la ruta es un endpoint de documentación que no debe requerir autenticación
     */
    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.startsWith("/api/auth") || // ¡LA LÍNEA CLAVE! Ignora todos los endpoints de autenticación.
               requestPath.startsWith("/swagger-ui") ||
               requestPath.startsWith("/v3/api-docs") ||
               requestPath.startsWith("/swagger-resources") ||
               requestPath.startsWith("/webjars") ||
               requestPath.startsWith("/redoc") ||
               requestPath.equals("/favicon.ico") ||
               requestPath.equals("/swagger-ui.html") ||
               requestPath.equals("/redoc.html");
    }
}