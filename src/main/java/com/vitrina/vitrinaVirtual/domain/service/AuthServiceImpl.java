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
        UserDto userDto = userRepository.findByUserName(loginRequestDto.getName());
        if (userDto == null) {
            throw new RuntimeException("Invalid credentials");
        }
        Usuario usuario = userRepository.findByUsernameFromEntity(loginRequestDto.getName());
        if (usuario == null || usuario.getContrasena() == null || 
            !passwordEncoder.matches(loginRequestDto.getPassword(), usuario.getContrasena())) {
            throw new RuntimeException("Invalid credentials");
        }
        return jwtTokenProvider.generateToken(userDto);
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
    public UserProfileDto getUserProfile(String username) {
        UserDto userDto = userRepository.findByUserName(username);
        if (userDto == null) {
            throw new RuntimeException("User not found");
        }
        UserProfileDto profile = new UserProfileDto();
        profile.setName(userDto.getName());
        profile.setEmail(userDto.getEmail());
        profile.setRole(userDto.getRole() != null ? userDto.getRole().name() : null); // Convertir RolJava a String
        return profile;
    }
    
}