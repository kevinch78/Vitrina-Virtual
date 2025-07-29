package com.vitrina.vitrinaVirtual.domain.dto;

import com.vitrina.vitrinaVirtual.infraestructura.entity.Rol;

import lombok.Data;

@Data
public class RegistrationRequestDto {
    private String name;
    private String password;
    private String email;
    private Rol role;
}
