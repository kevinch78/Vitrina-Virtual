package com.vitrina.vitrinaVirtual.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import com.vitrina.vitrinaVirtual.domain.dto.OutfitRecommendation;
import com.vitrina.vitrinaVirtual.domain.service.gemini.GeminiPromptBuilder;
import com.vitrina.vitrinaVirtual.domain.service.gemini.GeminiResponseParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

@Service
public class GeminiService {

    private static final Log logger = LogFactory.getLog(GeminiService.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GeminiPromptBuilder promptBuilder;
    private final GeminiResponseParser responseParser;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url}")
    private String apiBaseUrl;

    private static final String DEFAULT_MODEL = "gemini-2.0-flash";
    private static final int MIN_PRODUCTS_FOR_IA = 4; // Mínimo de productos para intentar llamar a la IA
    private static final int MAX_OUTPUT_TOKENS = 800;
    private static final double TEMPERATURE = 0.4; // Un poco más de creatividad
    private static final int TOP_K = 15;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper, GeminiPromptBuilder promptBuilder, GeminiResponseParser responseParser) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
    }

    public OutfitRecommendation getOutfitRecommendation(
            String gender,
            String climate,
            String style,
            String material,
            List<ProductWithStoreDto> products
    ) {
        // Si no hay suficientes productos, lanzamos una excepción clara. No hay fallback.
        if (products == null || products.size() < MIN_PRODUCTS_FOR_IA) {
            logger.warn("No hay suficientes productos para la IA (" + (products == null ? 0 : products.size()) + "). No se puede generar el outfit.");
            throw new OutfitGenerationException("No hay suficientes productos que coincidan con los criterios para generar un outfit.");
        }

        try {
            // Los productos ya vienen filtrados y optimizados desde ProductService. ¡Listos para la IA!
            String prompt = promptBuilder.buildCoherentOutfitPrompt(gender, climate, style, material, products);
            String apiUrl = buildApiUrl();
            logger.info("Calling Gemini API with " + products.size() + " products.");
            
            String response = callGeminiApi(apiUrl, prompt);
            
            // CORRECCIÓN: Crear ProductCategories en lugar de pasar null
            ProductCategories categories = createProductCategories(products);
            return responseParser.parseAndValidateResponse(response, products, categories);

        } catch (Exception e) {
            logger.error("Error generando recomendación de outfit con Gemini. Lanzando excepción.", e);
            // Envolvemos la excepción original para dar más contexto.
            throw new OutfitGenerationException("Falló la comunicación con el servicio de IA o la respuesta fue inválida.", e);
        }
    }

    // AGREGAR ESTE MÉTODO NUEVO
    private ProductCategories createProductCategories(List<ProductWithStoreDto> products) {
        ProductCategories categories = new ProductCategories();
        
        for (ProductWithStoreDto product : products) {
            if (product.getProduct() == null || product.getProduct().getGarmentType() == null) {
                continue;
            }
            
            String garmentType = product.getProduct().getGarmentType().toUpperCase();
            switch (garmentType) {
                case "TOP":
                    categories.getTops().add(product);
                    break;
                case "BOTTOM":
                    categories.getBottoms().add(product);
                    break;
                case "FOOTWEAR":
                    categories.getFootwear().add(product);
                    break;
                case "OUTERWEAR":
                    categories.getOuterwear().add(product);
                    break;
                case "ACCESSORY":
                    categories.getAccessories().add(product);
                    break;
                default:
                    categories.getOthers().add(product);
                    break;
            }
        }
        
        logger.info("Created ProductCategories - Tops: " + categories.getTops().size() + ", Bottoms: " + categories.getBottoms().size() + ", Footwear: " + categories.getFootwear().size() + ", Outerwear: " + categories.getOuterwear().size() + ", Accessories: " + categories.getAccessories().size() + ", Others: " + categories.getOthers().size());
        
        return categories;
    }

    // ============ LÓGICA DE COMPATIBILIDAD Y API ============

    private String buildApiUrl() {
        return apiBaseUrl + DEFAULT_MODEL + ":generateContent?key=" + apiKey;
    }

    private String callGeminiApi(String apiUrl, String prompt) throws Exception {
        Map<String, Object> requestBody = buildRequestBody(prompt);
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("API call failed with status: " + response.getStatusCode() + " and body: " + response.getBody());
        }
        return response.getBody();
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("role", "user", "parts", List.of(part));
        Map<String, Object> generationConfig = Map.of(
            "maxOutputTokens", MAX_OUTPUT_TOKENS,
            "temperature", TEMPERATURE,
            "topK", TOP_K
        );
        
        return Map.of("contents", List.of(content), "generationConfig", generationConfig);
    }

}