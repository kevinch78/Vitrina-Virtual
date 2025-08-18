package com.vitrina.vitrinaVirtual.infraestructura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long idProducto;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "precio")
    private Double precio;

    @Column(name = "existencia")
    private int existencia;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "estilo")
    private String estilo;

    @Column(name = "clima")
    private String clima;

    @Column(name = "genero")
    private String genero;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "color")
    private String color;

    @Column(name = "material")
    private String material;

    @Column(name = "ocasion")
    private String ocasion;

    @Column(name = "imagen_url", length = 512)
    private String imagenUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_almacen")
    private Almacen almacen;
}

