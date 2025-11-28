package com.vitrina.vitrinaVirtual.domain.dto;

import lombok.Data;

@Data
public class ProductDto {
    private Long idProduct;
    private String name;
    private Double price;
    private int stock;
    private String description;

    // Atributos de Estilismo
    private String style;       // E.g., "Urbano", "Bohemio", "Minimalista"
    private String climate;     // E.g., "Cálido", "Frío", "Templado"
    private String gender;      // E.g., "Masculino", "Femenino", "Unisex"
    private String occasion;    // E.g., "Fiesta", "Trabajo", "Casual"
    private String material;    // E.g., "Algodón", "Lana", "Seda"
    
    // Nuevos atributos para Outfits Inteligentes
    private String garmentType; // TIPO DE PRENDA (interno): "TOP", "BOTTOM", "FOOTWEAR", "OUTERWEAR", "ACCESSORY"
    private String subcategory; // Categoría original: "Camisa de manga larga", "Zapatillas deportivas"
    
    private String primaryColor;    // Color principal
    private String secondaryColors; // Colores secundarios (como string separado por comas)
    private String colorFamily;     // Familia de color: "Cálido", "Frío", "Neutro"
    
    private String pattern;     // Estampado: "Liso", "Rayas", "Cuadros", "Floral"
    private String fit;         // Corte: "Ajustado", "Regular", "Holgado", "Oversize"
    private Integer formality;  // Nivel de formalidad (1-5)
    private String iaDescription; //Descripcion creada por la IA despues de analizar la img
    private String technicalDescription; // Descripción técnica para la IA generadora de imágenes


    // Atributos existentes
    private String imageUrl;
    private Long storeId;
}
