package com.vitrina.vitrinaVirtual.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ClienteDto {

    private Long id;
    private String nombre;
    private String telefono;
    private String direccion;
    private Long usuarioId; // relación hacia el usuario
}


