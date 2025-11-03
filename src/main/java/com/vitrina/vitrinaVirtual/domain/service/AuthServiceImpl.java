package com.vitrina.vitrinaVirtual.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vitrina.vitrinaVirtual.domain.dto.LoginRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.RegistrationRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserProfileDto;
import com.vitrina.vitrinaVirtual.domain.repository.UserRepository;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Rol;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;
import com.vitrina.vitrinaVirtual.infraestructura.security.JwtTokenProvider;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public UserDto register(RegistrationRequestDto registrationRequestDto) {
        if (userRepository.findByUserName(registrationRequestDto.getName()) != null) {
            throw new RuntimeException("Username already exists");
        }
        if (registrationRequestDto.getRole() == null || !isValidRole(registrationRequestDto.getRole())) {
            throw new RuntimeException("Invalid role");
        }
        // Usa el método save que maneja RegistrationRequestDto directamente
        return userRepository.save(registrationRequestDto);
    }

    @Override
    public String login(LoginRequestDto loginRequestDto) {
        // 1. Buscamos al usuario por email UNA SOLA VEZ.
        Usuario usuario = userRepository.findByEmailFromEntity(loginRequestDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        // 2. Verificamos la contraseña.
        if (usuario.getContrasena() == null ||
            !passwordEncoder.matches(loginRequestDto.getPassword(), usuario.getContrasena())) {
            throw new RuntimeException("Credenciales inválidas");
        }
        // 3. Generamos el token directamente desde la entidad.
        return jwtTokenProvider.generateToken(usuario);
    }

    private boolean isValidRole(Rol role) {
        for (Rol validRole : Rol.values()) {
            if (validRole == role) {
                return true;
            }
        }
        return false;
    }
    @Override
    public UserProfileDto getUserProfile(String email) {
        // El 'username' que viene del token es en realidad el email. Buscamos por email.
        // Usamos findByEmailFromEntity para obtener la entidad y luego la mapeamos.
        Usuario usuario = userRepository.findByEmailFromEntity(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        // Mapeamos la entidad a un DTO de perfil
        UserProfileDto profile = new UserProfileDto();
        profile.setName(usuario.getNombre());
        profile.setEmail(usuario.getCorreo());
        profile.setRole(usuario.getRol() != null ? usuario.getRol().name() : null);
        return profile;
    }
    
}