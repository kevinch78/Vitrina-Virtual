package com.vitrina.vitrinaVirtual.infraestructura.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Almacen {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long almacenId;

    private String nombre;
    private String descripcion;
    private String ciudad;
    private String direccion;
    private String contacto;
    private String propietario;
    private Boolean publicidadActiva;
    private String imagenUrl;
}
