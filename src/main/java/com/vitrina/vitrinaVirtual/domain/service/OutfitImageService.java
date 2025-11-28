// package com.vitrina.vitrinaVirtual.domain.service;

// import com.vitrina.vitrinaVirtual.domain.dto.OutfitRecommendation;
// import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
// import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.*;
// import org.springframework.stereotype.Service;
// import org.springframework.util.LinkedMultiValueMap;
// import org.springframework.util.MultiValueMap;
// import org.springframework.web.client.RestTemplate;

// import java.util.*;

// @Service
// public class OutfitImageService {

//     private static final Log logger = LogFactory.getLog(OutfitImageService.class);
    
//     private final RestTemplate restTemplate;
//     private final CloudinaryService cloudinaryService;

//     @Value("${stability.api.key}")
//     private String stabilityApiKey;

//     @Value("${stability.api.base-url}")
//     private String stabilityApiBaseUrl;
    
//     @Value("${stability.model:sd3.5-large}")
//     private String stabilityModel;

//     // Configuración de generación de imágenes
//     private static final String OUTPUT_FORMAT = "png";
//     private static final String ASPECT_RATIO = "9:16"; // Vertical para outfit completo
//     private static final int MAX_RETRIES = 3;

//     public OutfitImageService(RestTemplate restTemplate, CloudinaryService cloudinaryService) {
//         this.restTemplate = restTemplate;
//         this.cloudinaryService = cloudinaryService;
//     }

//     /**
//      * Genera una imagen fotorealista del outfit usando Stable Diffusion 3.5.
//      * 
//      * @param outfit El outfit con los productos seleccionados
//      * @param gender Género del modelo (Masculino/Femenino)
//      * @param climate Clima para contexto visual (Frío/Templado/Cálido)
//      * @param style Estilo del outfit para ambiente fotográfico
//      * @return URL de la imagen generada y subida a Cloudinary
//      */
//     public String generateOutfitImage(OutfitRecommendation outfit, String gender, String climate, String style) {
//         try {
//             logger.info("=== INICIANDO GENERACIÓN DE IMAGEN ===");
//             logger.info("Productos en outfit: " + outfit.getSelectedProducts().size());
//             logger.info("Gender: " + gender + " | Climate: " + climate + " | Style: " + style);
            
//             // 1. Construir prompt fotorealista
//             String prompt = buildPhotorealisticPrompt(outfit, gender, climate, style);
//             logger.info("Prompt generado (longitud: " + prompt.length() + " chars)");
//             logger.debug("PROMPT COMPLETO: " + prompt);
            
//             // 2. Construir negative prompt para evitar defectos
//             String negativePrompt = buildNegativePrompt();
            
//             // 3. Llamar a Stable Diffusion con reintentos
//             byte[] imageBytes = callStableDiffusionWithRetry(prompt, negativePrompt);
//             logger.info("Imagen generada exitosamente (" + imageBytes.length + " bytes)");
            
//             // 4. Convertir a Base64
//             String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            
//             // 5. Subir a Cloudinary para persistencia
//             String publicId = "outfit_" + System.currentTimeMillis();
//             String cloudinaryUrl = cloudinaryService.uploadBase64Image(base64Image, "vitrina_virtual/outfits", publicId);
            
//             logger.info("=== IMAGEN SUBIDA EXITOSAMENTE ===");
//             logger.info("URL: " + cloudinaryUrl);
            
//             return cloudinaryUrl;
            
//         } catch (Exception e) {
//             logger.error("Error generando imagen del outfit", e);
//             // No lanzar excepción - que el outfit se muestre sin imagen
//             return null;
//         }
//     }

//     /**
//      * Construye un prompt fotorealista optimizado para Stable Diffusion.
//      * Usa las technicalDescription de cada producto para máxima precisión.
//      */
//     private String buildPhotorealisticPrompt(OutfitRecommendation outfit, String gender, String climate, String style) {
//         StringBuilder prompt = new StringBuilder();
        
//         // === SECCIÓN 1: CONFIGURACIÓN FOTOGRÁFICA BASE ===
//         prompt.append("professional fashion editorial photography, full body portrait, ");
        
//         // Modelo según género
//         if ("Femenino".equalsIgnoreCase(gender)) {
//             prompt.append("elegant female model, confident pose, ");
//         } else {
//             prompt.append("handsome male model, confident stance, ");
//         }
        
//         // === SECCIÓN 2: ILUMINACIÓN Y AMBIENTE ===
//         prompt.append(getPhotoStyle(style)).append(", ");
//         prompt.append(getClimateAmbiance(climate)).append(", ");
        
//         // === SECCIÓN 3: DESCRIPCIÓN DETALLADA DEL OUTFIT ===
//         prompt.append("wearing: ");
        
//         List<ProductWithStoreDto> products = outfit.getSelectedProducts();
//         for (int i = 0; i < products.size(); i++) {
//             ProductDto product = products.get(i).getProduct();
            
//             // Priorizar technicalDescription (creada específicamente para esto)
//             if (hasValue(product.getTechnicalDescription())) {
//                 prompt.append(product.getTechnicalDescription());
//             } else {
//                 // Fallback a construcción manual si no hay technical description
//                 prompt.append(buildManualDescription(product));
//             }
            
//             // Separador entre prendas
//             if (i < products.size() - 1) {
//                 prompt.append(", ");
//             }
//         }
        
//         // === SECCIÓN 4: CALIDAD TÉCNICA Y KEYWORDS DE SD ===
//         prompt.append(". ");
//         prompt.append("studio lighting, clean white background, sharp focus, ");
//         prompt.append("high resolution, 8k uhd, detailed fabric texture, ");
//         prompt.append("realistic materials and colors, professional color grading, ");
//         prompt.append("fashion magazine quality, photorealistic, masterpiece");
        
//         return prompt.toString();
//     }

//     /**
//      * Descripción manual de respaldo si no hay technicalDescription.
//      */
//     private String buildManualDescription(ProductDto product) {
//         StringBuilder desc = new StringBuilder();
        
//         // Tipo de prenda traducido al inglés
//         String garmentType = translateGarmentType(product.getGarmentType());
//         desc.append(garmentType);
        
//         // Color primario
//         if (hasValue(product.getPrimaryColor())) {
//             desc.append(" in ").append(product.getPrimaryColor().toLowerCase());
//         }
        
//         // Material
//         if (hasValue(product.getMaterial())) {
//             desc.append(", ").append(product.getMaterial().toLowerCase()).append(" material");
//         }
        
//         // Patrón (solo si no es liso)
//         if (hasValue(product.getPattern()) && !"Liso".equalsIgnoreCase(product.getPattern())) {
//             desc.append(" with ").append(product.getPattern().toLowerCase()).append(" pattern");
//         }
        
//         // Fit
//         if (hasValue(product.getFit())) {
//             desc.append(", ").append(translateFit(product.getFit())).append(" fit");
//         }
        
//         return desc.toString();
//     }

//     /**
//      * Negative prompt para evitar defectos comunes en SD.
//      * CRÍTICO para calidad fotorealista.
//      */
//     private String buildNegativePrompt() {
//         return "blurry, low quality, low resolution, distorted, deformed, disfigured, " +
//                "bad anatomy, bad proportions, extra limbs, missing limbs, extra fingers, " +
//                "poorly drawn hands, poorly drawn face, ugly, " +
//                "mutation, mutated, malformed, " +
//                "text, watermark, signature, logo, username, " +
//                "out of frame, cropped, cut off, " +
//                "worst quality, jpeg artifacts, duplicate, " +
//                "cartoon, anime, illustration, drawing, painting, sketch, 3d render, cgi, " +
//                "oversaturated, undersaturated, overexposed, underexposed";
//     }

//     /**
//      * Configura el estilo fotográfico según el tipo de outfit.
//      */
//     private String getPhotoStyle(String style) {
//         if (style == null) return "professional studio photography";
        
//         return switch (style.toLowerCase()) {
//             case "formal", "elegante" -> 
//                 "elegant high-fashion photography, dramatic studio lighting, luxury aesthetic, sophisticated backdrop";
//             case "deportivo" -> 
//                 "dynamic athletic photography, energetic lighting, modern clean background, sporty vibe";
//             case "casual" -> 
//                 "lifestyle photography, natural soft lighting, relaxed atmosphere, contemporary style";
//             case "urbano" -> 
//                 "urban street style photography, edgy lighting, modern city aesthetic, trendy look";
//             case "bohemio" -> 
//                 "artistic bohemian photography, warm natural lighting, free-spirited aesthetic";
//             default -> 
//                 "professional editorial photography, balanced studio lighting, timeless style";
//         };
//     }

//     /**
//      * Añade contexto visual según el clima.
//      */
//     private String getClimateAmbiance(String climate) {
//         if (climate == null) return "neutral temperature atmosphere";
        
//         return switch (climate.toLowerCase()) {
//             case "frío", "frio" -> 
//                 "winter season atmosphere, cool color tones, cozy indoor setting";
//             case "cálido", "calido" -> 
//                 "summer season atmosphere, warm color tones, bright natural lighting";
//             case "templado" -> 
//                 "spring/autumn atmosphere, balanced color tones, comfortable setting";
//             default -> 
//                 "neutral season, balanced lighting";
//         };
//     }

//     /**
//      * Traduce tipos de prenda al inglés para el prompt.
//      */
//     private String translateGarmentType(String garmentType) {
//         if (garmentType == null) return "clothing item";
        
//         return switch (garmentType.toUpperCase()) {
//             case "TOP" -> "shirt";
//             case "BOTTOM" -> "trousers";
//             case "DRESS" -> "dress";
//             case "OUTERWEAR" -> "jacket";
//             case "FOOTWEAR" -> "shoes";
//             case "ACCESSORY" -> "accessory";
//             default -> "garment";
//         };
//     }

//     /**
//      * Traduce el fit al inglés.
//      */
//     private String translateFit(String fit) {
//         if (fit == null) return "regular";
        
//         return switch (fit.toLowerCase()) {
//             case "ajustado" -> "fitted";
//             case "holgado" -> "loose";
//             case "oversize" -> "oversized";
//             default -> "regular";
//         };
//     }

//     /**
//      * Llama a Stable Diffusion con sistema de reintentos.
//      */
//     private byte[] callStableDiffusionWithRetry(String prompt, String negativePrompt) throws Exception {
//         Exception lastException = null;
        
//         for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
//             try {
//                 logger.info("Intento " + attempt + " de " + MAX_RETRIES + " - Llamando a Stability AI...");
//                 return callStableDiffusion(prompt, negativePrompt);
//             } catch (Exception e) {
//                 lastException = e;
//                 logger.warn("Intento " + attempt + " falló: " + e.getMessage());
                
//                 if (attempt < MAX_RETRIES) {
//                     // Esperar antes de reintentar (backoff exponencial)
//                     Thread.sleep(1000L * attempt);
//                 }
//             }
//         }
        
//         throw new OutfitGenerationException(
//             "Falló la generación de imagen después de " + MAX_RETRIES + " intentos", 
//             lastException
//         );
//     }

//     /**
//      * Realiza la llamada HTTP a la API de Stable Diffusion.
//      * Usa form-data porque SD3 lo requiere.
//      */
//     private byte[] callStableDiffusion(String prompt, String negativePrompt) throws Exception {
//         String apiUrl = stabilityApiBaseUrl + "/v2beta/stable-image/generate/sd3";
        
//         // Stable Diffusion 3 usa multipart/form-data
//         MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//         body.add("prompt", prompt);
//         body.add("negative_prompt", negativePrompt);
//         body.add("aspect_ratio", ASPECT_RATIO);
//         body.add("model", stabilityModel);
//         body.add("output_format", OUTPUT_FORMAT);
//         body.add("mode", "text-to-image");
        
//         HttpHeaders headers = new HttpHeaders();
//         headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//         headers.set("Authorization", "Bearer " + stabilityApiKey);
//         headers.set("Accept", "image/*");
        
//         HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
//         logger.debug("POST " + apiUrl);
        
//         ResponseEntity<byte[]> response = restTemplate.exchange(
//             apiUrl,
//             HttpMethod.POST,
//             requestEntity,
//             byte[].class
//         );
        
//         if (!response.getStatusCode().is2xxSuccessful()) {
//             String errorBody = response.getBody() != null ? new String(response.getBody()) : "No response body";
//             throw new RuntimeException("Stability API error: " + response.getStatusCode() + " - " + errorBody);
//         }
        
//         byte[] imageBytes = response.getBody();
//         if (imageBytes == null || imageBytes.length == 0) {
//             throw new RuntimeException("Stability API returned empty image");
//         }
        
//         return imageBytes;
//     }

//     private static boolean hasValue(String value) {
//         return value != null && !value.trim().isEmpty();
//     }
// }