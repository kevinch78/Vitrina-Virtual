package com.vitrina.vitrinaVirtual.domain.dto;

import com.vitrina.vitrinaVirtual.infraestructura.entity.Rol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long idUser;
    private String name;
    private String email;
    private Rol role; 
}
