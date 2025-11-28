package com.vitrina.vitrinaVirtual.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitrina.vitrinaVirtual.domain.dto.AuthResponse;
import com.vitrina.vitrinaVirtual.domain.dto.LoginRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.RegistrationRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserInfo;
import com.vitrina.vitrinaVirtual.domain.dto.UserProfileDto;
import com.vitrina.vitrinaVirtual.domain.repository.UserRepository;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Almacen;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Cliente;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Rol;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;
import com.vitrina.vitrinaVirtual.infraestructura.mapper.UserInfoMapper;
import com.vitrina.vitrinaVirtual.infraestructura.security.JwtTokenProvider;

import java.util.ArrayList;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    @Transactional
    public UserDto register(RegistrationRequestDto registrationRequestDto) {
        // Validar que el username no exista
        if (userRepository.findByUserName(registrationRequestDto.getName()) != null) {
            throw new RuntimeException("Username already exists");
        }
        
        // Validar que el email no exista
        if (userRepository.findByEmailFromEntity(registrationRequestDto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        
        // Validar rol
        if (registrationRequestDto.getRole() == null || !isValidRole(registrationRequestDto.getRole())) {
            throw new RuntimeException("Invalid role");
        }

        // Crear el usuario base
        Usuario usuario = new Usuario();
        usuario.setNombre(registrationRequestDto.getName());
        usuario.setCorreo(registrationRequestDto.getEmail());
        usuario.setContrasena(passwordEncoder.encode(registrationRequestDto.getPassword()));
        usuario.setRol(registrationRequestDto.getRole());

        // Según el rol, crear Cliente o Almacen
        if (registrationRequestDto.getRole() == Rol.CLIENT) {
            // Crear Cliente automáticamente
            Cliente cliente = new Cliente();
            cliente.setNombre(registrationRequestDto.getName());
            cliente.setTelefono("");
            cliente.setDireccion("");
            cliente.setUsuario(usuario); // Vinculación bidireccional
            
            usuario.setCliente(cliente);
            
        } else if (registrationRequestDto.getRole() == Rol.VENDOR) {
            // Crear Almacen automáticamente
            Almacen almacen = new Almacen();
            almacen.setNombre("Tienda de " + registrationRequestDto.getName());
            almacen.setDescripcion("Tienda sin descripción");
            almacen.setCiudad("");
            almacen.setDireccion("");
            almacen.setContacto(registrationRequestDto.getEmail());
            almacen.setImagenUrl(null);
            almacen.setPublicidadActiva(false);
            almacen.setPropietario(usuario); // Vinculación bidireccional
            almacen.setProductos(new ArrayList<>()); // Inicializar lista vacía
            
            usuario.setAlmacen(almacen);
        }
        // Si es ADMIN, no necesita Cliente ni Almacen

        // Guardar el usuario (cascade guarda Cliente o Almacen automáticamente)
        Usuario usuarioGuardado = userRepository.saveEntity(usuario);
        
        // Convertir a DTO y retornar
        UserDto userDto = new UserDto();
        userDto.setIdUser(usuarioGuardado.getIdUsuario());
        userDto.setName(usuarioGuardado.getNombre());
        userDto.setEmail(usuarioGuardado.getCorreo());
        userDto.setRole(usuarioGuardado.getRol());
            
        return userDto;
    }

    @Override
    public AuthResponse login(LoginRequestDto loginRequestDto) {
        // 1. Buscar usuario por email
        Usuario usuario = userRepository.findByEmailFromEntity(loginRequestDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        // 2. Verificar contraseña
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), usuario.getContrasena())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        // 3. Generar token
        String token = jwtTokenProvider.generateToken(usuario);

        // 4. Crear UserInfo
        UserInfo info = new UserInfo();
        info.setId(usuario.getIdUsuario());
        info.setEmail(usuario.getCorreo());
        info.setRole(usuario.getRol().name());
        
        // 5. Asignar storeId si es VENDOR y tiene almacen
        if (usuario.getRol() == Rol.VENDOR && usuario.getAlmacen() != null) {
            info.setStoreId(usuario.getAlmacen().getId());
        }
        
        // 6. Asignar clientId si es CLIENTE y tiene cliente
        if (usuario.getRol() == Rol.CLIENT && usuario.getCliente() != null) {
            info.setClientId(usuario.getCliente().getId());
        }

        // 6. Devolver AuthResponse
        return new AuthResponse(token, info);
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
        Usuario usuario = userRepository.findByEmailFromEntity(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        UserProfileDto profile = new UserProfileDto();
        profile.setName(usuario.getNombre());
        profile.setEmail(usuario.getCorreo());
        profile.setRole(usuario.getRol() != null ? usuario.getRol().name() : null);
        return profile;
    }
}