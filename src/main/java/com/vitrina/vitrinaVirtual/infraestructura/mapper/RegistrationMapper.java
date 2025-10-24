package com.vitrina.vitrinaVirtual.infraestructura.mapper;

import java.util.List;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vitrina.vitrinaVirtual.domain.dto.RegistrationRequestDto;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = "spring", uses = PasswordEncoder.class)
public interface RegistrationMapper {
    @Mapping(source = "nombre", target = "name")
    @Mapping(source = "correo", target = "email")
    @Mapping(source = "rol", target = "role")
    @Mapping(target = "password", ignore = true) // Ignora el campo password en el DTO de salida
    RegistrationRequestDto toRegistrationRequestDto (Usuario usuario);
    List<RegistrationRequestDto> toLoginRequestDtos (List<Usuario> usuarios);

    @InheritInverseConfiguration
    @Mapping(target = "idUsuario", ignore = true)
    @Mapping(target = "contrasena", expression = "java(org.springframework.util.StringUtils.hasText(registrationRequestDto.getPassword()) ? passwordEncoder.encode(registrationRequestDto.getPassword()) : null)")
    @Mapping(target = "nombre", expression = "java(registrationRequestDto.getName())") // Asegura texto plano
    @Mapping(target = "correo", expression = "java(registrationRequestDto.getEmail())") // Asegura texto plano    
    Usuario toUsuario(RegistrationRequestDto registrationRequestDto);
    List<Usuario> toUsuarios(List<RegistrationRequestDto> registrationRequestDtos);

}