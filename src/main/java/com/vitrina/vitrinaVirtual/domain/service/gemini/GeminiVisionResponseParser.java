package com.vitrina.vitrinaVirtual.domain.service.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitrina.vitrinaVirtual.domain.dto.ProductAnalysisDto;
import com.vitrina.vitrinaVirtual.domain.service.OutfitGenerationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class GeminiVisionResponseParser {

    private static final Log logger = LogFactory.getLog(GeminiVisionResponseParser.class);
    private final ObjectMapper objectMapper;

    public GeminiVisionResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parsea la respuesta JSON de Gemini Vision y la convierte en un DTO.
     * @param jsonResponse La respuesta en string de la API.
     * @return Un objeto ProductAnalysisDto.
     * @throws OutfitGenerationException si el parseo falla.
     */
    public ProductAnalysisDto parse(String jsonResponse) {
        try {
            // Extrae el contenido JSON, eliminando los ```json y ``` que a veces añade la IA.
            String cleanJson = GeminiResponseParser.extractJsonContent(jsonResponse);
            if (cleanJson == null || cleanJson.isBlank()) {
                logger.error("No se encontró contenido JSON válido en la respuesta de Gemini Vision: " + jsonResponse);
                throw new OutfitGenerationException("La respuesta de la IA para el análisis de producto estaba vacía o en un formato incorrecto.");
            }
            logger.debug("Clean JSON for parsing: " + cleanJson);
            return objectMapper.readValue(cleanJson, ProductAnalysisDto.class);
        } catch (Exception e) {
            logger.error("Error al parsear la respuesta de Gemini Vision. Respuesta recibida: " + jsonResponse, e);
            throw new OutfitGenerationException("No se pudo interpretar la respuesta de la IA para el análisis de producto.", e);
        }
    }
}