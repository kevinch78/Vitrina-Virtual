package com.vitrina.vitrinaVirtual.infraestructura.entity;


import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {
@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nombre", unique = true, nullable = false)
    private String nombre;

    @Column(name = "contraseña", nullable = false)
    private String contrasena;

    @Column(name = "correo", unique = true)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol")
    private Rol rol; // Usamos un enum para el rol

    // ✅ SOLUCIÓN: Agregar @Fetch(FetchMode.SELECT) para evitar duplicados
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    private Cliente cliente;

    @OneToOne(mappedBy = "propietario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    private Almacen almacen;

    // // CAMBIO IMPORTANTE: Agregar cascade y orphanRemoval
    // @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    // private Cliente cliente;

    // // CAMBIO IMPORTANTE: Ya estaba bien, pero verificar que tenga cascade
    // @OneToOne(mappedBy = "propietario", cascade = CascadeType.ALL, orphanRemoval = true)
    // private Almacen almacen;
}
