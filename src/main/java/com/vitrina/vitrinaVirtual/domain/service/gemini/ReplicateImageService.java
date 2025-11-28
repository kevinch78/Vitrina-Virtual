package com.vitrina.vitrinaVirtual.domain.service.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitrina.vitrinaVirtual.domain.dto.OutfitRecommendation;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import com.vitrina.vitrinaVirtual.domain.service.CloudinaryService;
import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ReplicateImageService {

    private static final Log logger = LogFactory.getLog(ReplicateImageService.class);

    private final RestTemplate restTemplate;
    private final CloudinaryService cloudinaryService;
    private final ObjectMapper objectMapper;

    @Value("${replicate.api.key}")
    private String replicateApiKey;

    @Value("${replicate.api.base-url:https://api.replicate.com/v1}")
    private String replicateApiBaseUrl;

    // FLUX.1 [dev] - Modelo gratuito y de alta calidad
    private static final String FLUX_MODEL_VERSION = "black-forest-labs/flux-dev";
    private static final int MAX_POLLING_ATTEMPTS = 60; // 60 segundos máximo
    private static final int POLLING_INTERVAL_MS = 1000; // Cada 1 segundo

    public ReplicateImageService(RestTemplate restTemplate, CloudinaryService cloudinaryService,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.cloudinaryService = cloudinaryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Genera una imagen fotorealista del outfit usando FLUX via Replicate.
     */
    public String generateOutfitImage(OutfitRecommendation outfit, String gender, String climate, String style) {
        try {
            logger.info("=== INICIANDO GENERACIÓN DE IMAGEN CON REPLICATE (FLUX) ===");
            logger.info("Productos en outfit: " + outfit.getSelectedProducts().size());

            // 1. Construir prompt (reutilizar lógica de StabilityAI)
            String prompt = buildPhotorealisticPrompt(outfit, gender, climate, style);
            logger.info("Prompt generado (longitud: " + prompt.length() + " chars)");

            // 2. Iniciar predicción en Replicate
            String predictionId = startPrediction(prompt);
            logger.info("Predicción iniciada con ID: " + predictionId);

            // 3. Esperar a que se complete (polling)
            String imageUrl = pollForCompletion(predictionId);
            logger.info("Imagen generada: " + imageUrl);

            // 4. Descargar y convertir a Base64
            String base64Image = downloadAndConvertToBase64(imageUrl);

            // 5. Subir a Cloudinary
            String publicId = "outfit_flux_" + System.currentTimeMillis();
            String cloudinaryUrl = cloudinaryService.uploadBase64Image(base64Image, "vitrina_virtual/outfits",
                    publicId);

            logger.info("=== IMAGEN SUBIDA EXITOSAMENTE ===");
            logger.info("URL: " + cloudinaryUrl);

            return cloudinaryUrl;

        } catch (Exception e) {
            logger.error("Error generando imagen con Replicate", e);
            return null;
        }
    }

    /**
     * Inicia una predicción en Replicate y devuelve el ID.
     */
    private String startPrediction(String prompt) throws Exception {
        String apiUrl = replicateApiBaseUrl + "/predictions";

        Map<String, Object> input = Map.of(
                "prompt", prompt,
                // "aspect_ratio", "9:16",
                "width", 480,
                "height", 854,
                "output_format", "png",
                "output_quality", 90,
                "num_inference_steps", 24, // Balance calidad/velocidad
                "guidance_scale", 3.5);

        Map<String, Object> requestBody = Map.of(
                "version", FLUX_MODEL_VERSION,
                "input", input);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + replicateApiKey);
        headers.set("Prefer", "wait"); // Intenta esperar respuesta inmediata

        HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(requestBody),
                headers);

        logger.debug("POST " + apiUrl);
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Replicate API error: " + response.getStatusCode());
        }

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        return responseJson.path("id").asText();
    }

    /**
     * Hace polling hasta que la imagen esté lista.
     */
    private String pollForCompletion(String predictionId) throws Exception {
        String statusUrl = replicateApiBaseUrl + "/predictions/" + predictionId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + replicateApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (int attempt = 0; attempt < MAX_POLLING_ATTEMPTS; attempt++) {
            logger.debug("Polling intento " + (attempt + 1) + "/" + MAX_POLLING_ATTEMPTS);

            ResponseEntity<String> response = restTemplate.exchange(
                    statusUrl, HttpMethod.GET, entity, String.class);

            JsonNode statusJson = objectMapper.readTree(response.getBody());
            String status = statusJson.path("status").asText();

            logger.debug("Status: " + status);

            if ("succeeded".equals(status)) {
                // La salida puede ser un string o un array
                JsonNode output = statusJson.path("output");
                if (output.isArray() && output.size() > 0) {
                    return output.get(0).asText();
                } else if (output.isTextual()) {
                    return output.asText();
                }
                throw new RuntimeException("Output format desconocido");
            } else if ("failed".equals(status) || "canceled".equals(status)) {
                String error = statusJson.path("error").asText("Unknown error");
                throw new RuntimeException("Generación falló: " + error);
            }

            // Esperar antes del siguiente intento
            Thread.sleep(POLLING_INTERVAL_MS);
        }

        throw new RuntimeException("Timeout esperando generación de imagen (60s)");
    }

    /**
     * Descarga la imagen desde URL y convierte a Base64.
     */
    private String downloadAndConvertToBase64(String imageUrl) throws Exception {
        logger.info("Descargando imagen desde: " + imageUrl);

        ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error descargando imagen desde Replicate");
        }

        return Base64.getEncoder().encodeToString(response.getBody());
    }

    // ============ MÉTODOS REUTILIZADOS DE STABILITY AI ============

    private String buildPhotorealisticPrompt(OutfitRecommendation outfit, String gender, String climate, String style) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("professional fashion editorial photography, full body portrait, ");

        if ("Femenino".equalsIgnoreCase(gender)) {
            prompt.append("elegant female model, confident pose, ");
        } else {
            prompt.append("handsome male model, confident stance, ");
        }

        prompt.append(getPhotoStyle(style)).append(", ");
        prompt.append(getClimateAmbiance(climate)).append(", ");

        prompt.append("wearing: ");

        List<ProductWithStoreDto> products = outfit.getSelectedProducts();
        for (int i = 0; i < products.size(); i++) {
            ProductDto product = products.get(i).getProduct();

            if (hasValue(product.getTechnicalDescription())) {
                prompt.append(product.getTechnicalDescription());
            } else {
                prompt.append(buildManualDescription(product));
            }

            if (i < products.size() - 1) {
                prompt.append(", ");
            }
        }

        prompt.append(". ");
        prompt.append("studio lighting, clean white background, sharp focus, ");
        prompt.append("high resolution, detailed fabric texture, ");
        prompt.append("realistic materials and colors, professional color grading, ");
        prompt.append("fashion magazine quality, photorealistic");

        return prompt.toString();
    }

    private String buildManualDescription(ProductDto product) {
        StringBuilder desc = new StringBuilder();

        String garmentType = translateGarmentType(product.getGarmentType());
        desc.append(garmentType);

        if (hasValue(product.getPrimaryColor())) {
            desc.append(" in ").append(product.getPrimaryColor().toLowerCase());
        }

        if (hasValue(product.getMaterial())) {
            desc.append(", ").append(product.getMaterial().toLowerCase()).append(" material");
        }

        if (hasValue(product.getPattern()) && !"Liso".equalsIgnoreCase(product.getPattern())) {
            desc.append(" with ").append(product.getPattern().toLowerCase()).append(" pattern");
        }

        if (hasValue(product.getFit())) {
            desc.append(", ").append(translateFit(product.getFit())).append(" fit");
        }

        return desc.toString();
    }

    private String getPhotoStyle(String style) {
        if (style == null)
            return "professional studio photography";

        return switch (style.toLowerCase()) {
            case "formal", "elegante" ->
                "elegant high-fashion photography, dramatic studio lighting, luxury aesthetic";
            case "deportivo" ->
                "dynamic athletic photography, energetic lighting, modern clean background";
            case "casual" ->
                "lifestyle photography, natural soft lighting, relaxed atmosphere";
            case "urbano" ->
                "urban street style photography, edgy lighting, trendy look";
            default ->
                "professional editorial photography, balanced studio lighting";
        };
    }

    private String getClimateAmbiance(String climate) {
        if (climate == null)
            return "neutral temperature atmosphere";

        return switch (climate.toLowerCase()) {
            case "frío", "frio" -> "winter season atmosphere, cool color tones";
            case "cálido", "calido" -> "summer season atmosphere, warm color tones";
            case "templado" -> "spring/autumn atmosphere, balanced color tones";
            default -> "neutral season, balanced lighting";
        };
    }

    private String translateGarmentType(String garmentType) {
        if (garmentType == null)
            return "clothing item";

        return switch (garmentType.toUpperCase()) {
            case "TOP" -> "shirt";
            case "BOTTOM" -> "trousers";
            case "DRESS" -> "dress";
            case "OUTERWEAR" -> "jacket";
            case "FOOTWEAR" -> "shoes";
            case "ACCESSORY" -> "accessory";
            default -> "garment";
        };
    }

    private String translateFit(String fit) {
        if (fit == null)
            return "regular";

        return switch (fit.toLowerCase()) {
            case "ajustado" -> "fitted";
            case "holgado" -> "loose";
            case "oversize" -> "oversized";
            default -> "regular";
        };
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
