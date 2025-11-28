package com.vitrina.vitrinaVirtual.infraestructura.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vitrina.vitrinaVirtual.domain.dto.UserInfo;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;

@Mapper(componentModel = "spring")
public interface UserInfoMapper {

    @Mapping(source = "idUsuario", target = "id")
    @Mapping(source = "correo", target = "email")
    @Mapping(source = "rol", target = "role")
    @Mapping(target = "storeId", ignore = true) 
    UserInfo toUserInfo(Usuario usuario);
}
