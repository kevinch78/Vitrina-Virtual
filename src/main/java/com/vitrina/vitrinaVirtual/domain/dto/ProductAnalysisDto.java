package com.vitrina.vitrinaVirtual.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO que representa la respuesta completa del análisis de un producto por parte de la IA.
 */
@Data
public class ProductAnalysisDto {

    @JsonProperty("attributes")
    private ProductAttributesDto attributes;

    @JsonProperty("detailedDescription")
    private String detailedDescription;

    @JsonProperty("technicalDescription")
    private String technicalDescription;

    /**
     * DTO anidado para los atributos estructurados del producto.
     */
    @Data
    public static class ProductAttributesDto {
        private String style, garmentType, colorFamily, material, pattern, fit;
        private Integer formality;
    }
}