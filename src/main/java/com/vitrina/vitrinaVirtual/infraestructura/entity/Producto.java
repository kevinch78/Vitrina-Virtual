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

    // Atributos de Estilismo
    @Column(name = "estilo")
    private String estilo;

    @Column(name = "clima")
    private String clima;

    @Column(name = "genero")
    private String genero;

    @Column(name = "ocasion")
    private String ocasion;

    @Column(name = "material")
    private String material;

    // Nuevos atributos para Outfits Inteligentes
    @Column(name = "garment_type")
    private String garmentType;

    @Column(name = "subcategoria")
    private String subcategoria;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_colors")
    private String secondaryColors;

    @Column(name = "color_family")
    private String colorFamily;

    @Column(name = "pattern")
    private String pattern;

    @Column(name = "fit")
    private String fit;

    @Column(name = "formality")
    private Integer formality;

    /**
     * Descripción detallada y de marketing del producto, generada por la IA.
     * Se utiliza para construir prompts de alta calidad para la generación de
     * imágenes.
     */
    @Column(name = "ia_description", columnDefinition = "TEXT")
    private String iaDescription;

    /**
     * Descripción técnica y visual del producto, generada por la IA.
     * Se utiliza para construir prompts de alta calidad para la generación de imágenes.
     */
    @Column(name = "technical_description", columnDefinition = "TEXT")
    private String technicalDescription;

    // Atributos existentes
    @Column(name = "imagen_url", length = 512)
    private String imagenUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_almacen")
    private Almacen almacen;
}
