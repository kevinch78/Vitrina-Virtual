package com.vitrina.vitrinaVirtual.infraestructura.repositories;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.vitrina.vitrinaVirtual.domain.dto.LoginRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.RegistrationRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserDto;
import com.vitrina.vitrinaVirtual.domain.repository.UserRepository;
import com.vitrina.vitrinaVirtual.infraestructura.crud_interface.UsuarioCrudRepository;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;
import com.vitrina.vitrinaVirtual.infraestructura.mapper.RegistrationMapper;
import com.vitrina.vitrinaVirtual.infraestructura.mapper.SolicitudMapper;
import com.vitrina.vitrinaVirtual.infraestructura.mapper.UsuarioMapper;

@Repository
public class UsuarioRepository implements UserRepository {
    @Autowired
    private UsuarioCrudRepository usuarioCrudRepository;
    @Autowired
    private UsuarioMapper usuarioMapper;
    @Autowired
    private SolicitudMapper solicitudMapper;
    // @Autowired
    // private PasswordEncoder passwordEncoder;
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
    public UserDto findByUserName(String username) {
        return usuarioMapper.toUserDto(
            usuarioCrudRepository.findByNombre(username)
        );
    }
    @Override
    public LoginRequestDto findLoginRequestByUsername(String username) {
        Usuario usuario = usuarioCrudRepository.findByNombre(username);
        return usuario != null ? solicitudMapper.toLoginRequestDto(usuario) : null;
    }
    @Override
    public List<UserDto> findAll() {
        List<Usuario> usuario = usuarioCrudRepository.findAll();
        return usuarioMapper.toUserDtos(usuario);
    }
    @Override
    public Usuario findByUsernameFromEntity(String username) {
        return usuarioCrudRepository.findByNombre(username);
    }
    

}
