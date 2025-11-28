package com.vitrina.vitrinaVirtual.infraestructura.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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
public class Almacen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "almacen_id")
    private Long id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "ciudad")
    private String ciudad;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "contacto")
    private String contacto;

    @Column(name = "imagen_url", length = 512)
    private String imagenUrl;

    @Column(name = "publicidad_activa", columnDefinition = "TINYINT(1)")
    private Boolean publicidadActiva;

    // ✅ SOLUCIÓN: Agregar fetch = LAZY y @Fetch(FetchMode.SELECT)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    @Fetch(FetchMode.SELECT)
    private Usuario propietario;

    @OneToMany(mappedBy = "almacen", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @Builder.Default
    private List<Producto> productos = new ArrayList<>();

    // @OneToOne
    // @JoinColumn(name = "user_id", unique = true)
    // private Usuario propietario;

    // // IMPORTANTE: Inicializar la lista para evitar NullPointerException
    // @OneToMany(mappedBy = "almacen", cascade = CascadeType.ALL, orphanRemoval = true)
    // @Builder.Default
    // private List<Producto> productos = new ArrayList<>();
}