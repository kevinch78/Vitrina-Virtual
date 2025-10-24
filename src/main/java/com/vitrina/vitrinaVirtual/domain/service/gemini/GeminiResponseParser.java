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

            JsonNode root = objectMapper.readTree(response);
            String text = extractTextFromResponse(root);

            if (text == null || text.isBlank()) {
                throw new OutfitGenerationException("No se pudo extraer el contenido de texto de la respuesta de la IA.");
            }

            String jsonBlock = extractFirstJsonObject(stripCodeFences(text).trim());
            if (jsonBlock == null) {
                throw new OutfitGenerationException("La respuesta de la IA no contenía un bloque JSON válido.");
            }

            JsonNode outfitNode = objectMapper.readTree(jsonBlock);

            List<ProductWithStoreDto> selectedProducts = extractSelectedProducts(outfitNode, products);
            // Ya no completamos el outfit si viene incompleto. Confiamos en la IA.
            // selectedProducts = validateAndCompleteOutfit(selectedProducts, categories);
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

    private String extractTextFromResponse(JsonNode root) {
        return Optional.ofNullable(
            root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null)
        ).orElse(null);
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
}
