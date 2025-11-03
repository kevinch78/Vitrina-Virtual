// package com.vitrina.vitrinaVirtual.infraestructura.mapper;

// import java.util.List;

// import org.mapstruct.InheritInverseConfiguration;
// import org.mapstruct.Mapper;
// import org.mapstruct.Mapping;

// import com.vitrina.vitrinaVirtual.domain.dto.LoginRequestDto;
// import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;

// @Mapper(componentModel = "spring")
// public interface SolicitudMapper {
//     @Mapping(source = "nombre", target = "name")
//     @Mapping(source = "contrasena", target = "password")
//     LoginRequestDto toLoginRequestDto(Usuario usuario);

//     List<LoginRequestDto> toLoginRequestDtos(List<Usuario> usuarios);

//     @InheritInverseConfiguration
//     @Mapping(target = "idUsuario", ignore = true) // Ignorar el ID
//     @Mapping(target = "correo", ignore = true)   // Ignorar el correo
//     @Mapping(target = "rol", ignore = true)     // Ignorar el rol
//     @Mapping(target = "contrasena", ignore = true) // Ignorar la contraseña (opcional, ya que se mapea)
//     Usuario toUsuario(LoginRequestDto loginRequestDto);

//     List<Usuario> toUsuarios(List<LoginRequestDto> loginRequestDtos);
// }