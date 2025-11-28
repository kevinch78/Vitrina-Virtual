package com.vitrina.vitrinaVirtual.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitrina.vitrinaVirtual.domain.dto.ProductAnalysisDto;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import com.vitrina.vitrinaVirtual.domain.dto.OutfitRecommendation;
import com.vitrina.vitrinaVirtual.domain.service.gemini.GeminiPromptBuilder;
import com.vitrina.vitrinaVirtual.domain.service.gemini.GeminiResponseParser;
import com.vitrina.vitrinaVirtual.domain.service.gemini.GeminiVisionPromptBuilder;
import com.vitrina.vitrinaVirtual.domain.service.gemini.GeminiVisionResponseParser;
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
    private final GeminiVisionPromptBuilder visionPromptBuilder;
    private final GeminiVisionResponseParser visionResponseParser;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url}")
    private String apiBaseUrl;
    
    @Value("${gemini.api.text-model}")
    private String textModel;

    @Value("${gemini.api.vision-model}")
    private String visionModel;

    private static final int MIN_PRODUCTS_FOR_IA = 4; // Mínimo de productos para intentar llamar a la IA
    private static final int MAX_OUTPUT_TOKENS = 800;
    private static final double TEMPERATURE = 0.4; // Un poco más de creatividad
    private static final int TOP_K = 15;

    public GeminiService(RestTemplate restTemplate,
                         ObjectMapper objectMapper,
                         GeminiPromptBuilder promptBuilder,
                         GeminiResponseParser responseParser,
                         GeminiVisionPromptBuilder visionPromptBuilder,
                         GeminiVisionResponseParser visionResponseParser) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.visionPromptBuilder = visionPromptBuilder;
        this.visionResponseParser = visionResponseParser;
    }

    public OutfitRecommendation getOutfitRecommendation(
            String gender, String climate, String style, String material, List<ProductWithStoreDto> products) {
        // Si no hay suficientes productos, lanzamos una excepción clara. No hay fallback.
        if (products == null || products.size() < MIN_PRODUCTS_FOR_IA) {
            logger.warn("No hay suficientes productos para la IA (" + (products == null ? 0 : products.size()) + "). No se puede generar el outfit.");
            throw new OutfitGenerationException("No hay suficientes productos que coincidan con los criterios para generar un outfit.");
        }

        try {
            // Los productos ya vienen filtrados y optimizados desde ProductService. ¡Listos para la IA!
            String prompt = promptBuilder.buildCoherentOutfitPrompt(gender, climate, style, material, products);
            String apiUrl = buildApiUrl(textModel);
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

    /**
     * Analiza una imagen de producto utilizando Gemini Vision para extraer atributos
     * y generar una descripción de marketing.
     *
     * @param imageBase64 String en formato Base64 de la imagen del producto.
     * @param productName Nombre del producto, para dar contexto a la IA.
     * @return Un DTO {@link ProductAnalysisDto} con los atributos y la descripción generada.
     * @throws OutfitGenerationException si la comunicación con la API falla o la respuesta es inválida.
     */
    public ProductAnalysisDto analyzeProductImage(String imageBase64, String productName) {
        logger.info("Analyzing product image for: " + productName);
        try {
            String prompt = visionPromptBuilder.buildProductAnalysisPrompt(productName);
            String apiUrl = buildApiUrl(visionModel);
            String requestBody = buildVisionRequestBody(prompt, imageBase64);
            String response = callApi(apiUrl, requestBody);
            return visionResponseParser.parse(response);
        } catch (Exception e) {
            logger.error("Error analyzing product image with Gemini Vision. Product: " + productName, e);
            throw new OutfitGenerationException("Falló el análisis de la imagen del producto con la IA.", e);
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

    private String buildApiUrl(String model) {
        return apiBaseUrl + model + ":generateContent?key=" + apiKey;
    }

    private String callGeminiApi(String apiUrl, String prompt) throws Exception {
        String requestBodyJson = buildTextRequestBody(prompt);
        return callApi(apiUrl, requestBodyJson);
    }

    private String callApi(String apiUrl, String requestBodyJson) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("API call failed with status: " + response.getStatusCode() + " and body: " + response.getBody());
        }
        return response.getBody();
    }

    private String buildTextRequestBody(String prompt) throws Exception {
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("role", "user", "parts", List.of(part));
        Map<String, Object> generationConfig = Map.of(
            "maxOutputTokens", MAX_OUTPUT_TOKENS,
            "temperature", TEMPERATURE,
            "topK", TOP_K
        );
        
        Map<String, Object> requestBody = Map.of("contents", List.of(content), "generationConfig", generationConfig);
        return objectMapper.writeValueAsString(requestBody);
    }

    private String buildVisionRequestBody(String prompt, String imageBase64) throws Exception {
        // Parte 1: El texto del prompt
        Map<String, Object> textPart = Map.of("text", prompt);

        // Parte 2: La imagen. Le pasamos los datos en Base64.
        Map<String, Object> imageData = Map.of("mime_type", "image/jpeg", "data", imageBase64);
        Map<String, Object> imagePart = Map.of("inline_data", imageData);

        // El orden importa: primero el texto (instrucciones), luego la imagen (el sujeto).
        List<Map<String, Object>> parts = List.of(textPart, imagePart); // Correcto: texto, luego imagen
        Map<String, Object> content = Map.of("parts", parts);

        // Para asegurar que la respuesta sea siempre JSON, añadimos una generationConfig.
        Map<String, Object> generationConfig = Map.of(
            "maxOutputTokens", MAX_OUTPUT_TOKENS,
            "temperature", TEMPERATURE,
            "topK", TOP_K,
            "responseMimeType", "application/json" // ¡Clave para forzar la salida JSON!
        );

        Map<String, Object> requestBody = Map.of("contents", List.of(content), "generationConfig", generationConfig);

        return objectMapper.writeValueAsString(requestBody);
    }
}