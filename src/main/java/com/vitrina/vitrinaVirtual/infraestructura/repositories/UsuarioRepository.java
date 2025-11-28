package com.vitrina.vitrinaVirtual.infraestructura.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.vitrina.vitrinaVirtual.domain.dto.RegistrationRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserDto;
import com.vitrina.vitrinaVirtual.domain.repository.UserRepository;
import com.vitrina.vitrinaVirtual.infraestructura.crud_interface.UsuarioCrudRepository;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;
import com.vitrina.vitrinaVirtual.infraestructura.mapper.UsuarioMapper;
import com.vitrina.vitrinaVirtual.infraestructura.mapper.RegistrationMapper;

@Repository
public class UsuarioRepository implements UserRepository {
    @Autowired
    private UsuarioCrudRepository usuarioCrudRepository;
    @Autowired
    private UsuarioMapper usuarioMapper;
    @Autowired
    private RegistrationMapper registrationMapper;

    @Override
    public UserDto save(UserDto userDto) {
        Usuario usuario = usuarioMapper.toUsuario(userDto);
        usuario = usuarioCrudRepository.save(usuario);
        return usuarioMapper.toUserDto(usuario);
    }

    @Override
    public UserDto save(RegistrationRequestDto registrationRequestDto) {
        if (registrationRequestDto == null || registrationRequestDto.getPassword() == null || registrationRequestDto.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required and cannot be empty");
        }
        Usuario usuario = registrationMapper.toUsuario(registrationRequestDto);
        System.out.println("Contraseña encriptada antes de guardar: " + (usuario.getContrasena() != null ? usuario.getContrasena() : "null"));
        // Verifica que nombre y correo no sean hashes
        if (usuario.getNombre() != null && usuario.getNombre().startsWith("$2a$")) {
            throw new IllegalStateException("Nombre no debería estar encriptado: " + usuario.getNombre());
        }
        if (usuario.getCorreo() != null && usuario.getCorreo().startsWith("$2a$")) {
            throw new IllegalStateException("Correo no debería estar encriptado: " + usuario.getCorreo());
        }
        usuario = usuarioCrudRepository.save(usuario);
        return usuarioMapper.toUserDto(usuario);
    }
    @Override
    public Usuario saveEntity(Usuario usuario) {
        return usuarioCrudRepository.save(usuario);
    }

    @Override
    public UserDto findByUserName(String username) {
        return usuarioCrudRepository.findByNombre(username)
            .map(usuarioMapper::toUserDto)
            .orElse(null); // Devuelve null si no se encuentra
    }

    @Override
    public List<UserDto> findAll() {
        List<Usuario> usuarios = (List<Usuario>) usuarioCrudRepository.findAll();
        return usuarioMapper.toUserDtos(usuarios);
    }

    @Override
    public Usuario findByUsernameFromEntity(String username) {
        return usuarioCrudRepository.findByNombre(username)
            .orElse(null); // Devuelve null si no se encuentra
    }

    @Override
    public Optional<Usuario> findByEmailFromEntity(String email) {
        return usuarioCrudRepository.findByCorreo(email);
    }
}