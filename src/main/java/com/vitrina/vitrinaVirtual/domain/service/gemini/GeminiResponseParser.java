package com.vitrina.vitrinaVirtual.domain.service.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitrina.vitrinaVirtual.domain.dto.OutfitRecommendation;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import com.vitrina.vitrinaVirtual.domain.service.OutfitGenerationException;
import com.vitrina.vitrinaVirtual.domain.service.ProductCategories;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GeminiResponseParser {

    private final ObjectMapper objectMapper;

    public GeminiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutfitRecommendation parseAndValidateResponse(String response, List<ProductWithStoreDto> products, ProductCategories categories) {
        try {
            if (response == null || response.isBlank()) {
                throw new OutfitGenerationException("La respuesta de la IA estaba vacía.");
            }

            // Usamos el método estático refactorizado para limpiar la respuesta.
            String jsonBlock = extractJsonContent(response);
            if (jsonBlock == null) {
                throw new OutfitGenerationException("La respuesta de la IA no contenía un bloque JSON válido.");
            }

            JsonNode outfitNode = objectMapper.readTree(jsonBlock);

            List<ProductWithStoreDto> selectedProducts = extractSelectedProducts(outfitNode, products);
            String accesorio = extractAccessory(outfitNode, categories);

            return new OutfitRecommendation(selectedProducts, accesorio);

        } catch (Exception e) {
            // Envolvemos la excepción original para no perder la causa raíz del error.
            throw new OutfitGenerationException("Error al procesar la respuesta de la IA: " + e.getMessage(), e);
        }
    }

    private List<ProductWithStoreDto> extractSelectedProducts(JsonNode outfitNode, List<ProductWithStoreDto> products) {
        List<String> prendasNames = new ArrayList<>();
        if (outfitNode.has("prendas") && outfitNode.get("prendas").isArray()) {
            outfitNode.get("prendas").forEach(node -> {
                if (node.isTextual()) {
                    prendasNames.add(node.asText().trim());
                }
            });
        }

        Map<String, ProductWithStoreDto> productMap = products.stream()
            .filter(p -> p.getProduct() != null && p.getProduct().getName() != null)
            .collect(Collectors.toMap(
                p -> p.getProduct().getName(),
                p -> p,
                (existing, replacement) -> existing
            ));

        return prendasNames.stream()
            .map(productMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private String extractAccessory(JsonNode outfitNode, ProductCategories categories) {
        String accesorio = "Sugerencia externa: Accesorio complementario";
        if (outfitNode.has("accesorio") && outfitNode.get("accesorio").isTextual()) {
            accesorio = outfitNode.get("accesorio").asText().trim();
        }

        final String finalAccesorio = accesorio;
        boolean accessoryExists = categories.getAccessories().stream()
            .anyMatch(p -> p.getProduct().getName().equals(finalAccesorio));

        if (!accessoryExists && !accesorio.toLowerCase().startsWith("sugerencia externa:")) {
            accesorio = "Sugerencia externa: " + accesorio;
        }

        return accesorio;
    }

    private static String stripCodeFences(String s) {
        if (s == null) return "";
        String cleaned = s.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
            }
        }
        return cleaned.trim();
    }

    private static String extractFirstJsonObject(String s) {
        if (s == null) return null;
        int start = s.indexOf('{');
        if (start < 0) return null;

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;

            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return s.substring(start, i + 1);
            }
        }
        return null;
    }

    /**
     * Extrae el bloque de texto JSON principal de la respuesta completa de la API de Gemini.
     * Este método es público y estático para que pueda ser reutilizado por otros parsers.
     * @param fullApiResponse La respuesta completa de la API.
     * @return El string JSON limpio, o null si no se encuentra.
     */
    public static String extractJsonContent(String fullApiResponse) {
        try {
            // Intenta parsear la respuesta completa como si fuera la estructura de Gemini
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(fullApiResponse);
            String text = Optional.ofNullable(
                root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null)
            ).orElse(null);

            if (text != null && !text.isBlank()) {
                // Si se extrajo texto, se limpia y se busca el JSON dentro.
                return extractFirstJsonObject(stripCodeFences(text).trim());
            }
        } catch (Exception e) {
            // Si el parseo falla, es probable que la respuesta ya sea el JSON (o basura).
        }
        // Como fallback, intenta limpiar la respuesta original directamente.
        return extractFirstJsonObject(stripCodeFences(fullApiResponse).trim());
    }
}
