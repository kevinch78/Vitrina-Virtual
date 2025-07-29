package com.vitrina.vitrinaVirtual.domain.dto;

import com.vitrina.vitrinaVirtual.infraestructura.entity.Rol;

import lombok.Data;

@Data
public class UserDto {
    private Long idUser;
    private String name;
    private String email;
    private Rol role; 
}
