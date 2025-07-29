// package com.vitrina.vitrinaVirtual.infraestructura.entity;

// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.ManyToOne;
// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;

// @Entity
// @Data
// @NoArgsConstructor
// @AllArgsConstructor
// @Builder
// public class Producto {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     private String nombre;
//     private Double precio;
//     private int existencia;
//     private String descripcion;
//     private String estilo; //estilo : casual, formal
//     private String clima; // frio, calido
//     private String genero;//masculino ,femenimo, unisex
//     private String categoria;//ropa, accesorios, calzado, etc.
//     private String color;
//     private String material;
//     private String ocasion;

//     private String imagenUrl; // aquí guardas el link o nombre del archivo

//     // @ManyToOne
//     // private Almacen almacen;
// }

