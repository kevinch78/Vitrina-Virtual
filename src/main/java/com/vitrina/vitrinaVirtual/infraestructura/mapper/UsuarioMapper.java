package com.vitrina.vitrinaVirtual.infraestructura.mapper;

import java.util.List;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vitrina.vitrinaVirtual.domain.dto.UserDto;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(source = "idUsuario", target = "idUser")
    @Mapping(source = "nombre", target = "name")
    @Mapping(source = "correo", target = "email")
    @Mapping(source = "rol", target = "role")

    UserDto toUserDto(Usuario usuario);

    List<UserDto> toUserDtos(List<Usuario> usuarios);

    @InheritInverseConfiguration
    @Mapping(target = "contrasena", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "almacen", ignore = true)
    Usuario toUsuario(UserDto userDto);

    List<Usuario> toUsuarios(List<UserDto> userDtos);
}
