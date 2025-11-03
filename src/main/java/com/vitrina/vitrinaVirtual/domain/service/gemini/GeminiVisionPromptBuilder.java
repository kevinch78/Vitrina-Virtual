package com.vitrina.vitrinaVirtual.domain.service.gemini;

import org.springframework.stereotype.Component;

@Component
public class GeminiVisionPromptBuilder {

    /**
     * Construye un prompt robusto para el análisis de una imagen de producto.
     * Este prompt está diseñado para forzar una salida JSON estricta.
     *
     * @param productName El nombre del producto para dar contexto.
     * @return El prompt completo como un String.
     */
    public String buildProductAnalysisPrompt(String productName) {
        // Usamos un bloque de texto de Java para mayor claridad.
        return """
        TASK: Analyze the clothing item in the image and provide its attributes in a strict JSON format.
        PRODUCT NAME: "%s"

        INSTRUCTIONS:
        1.  Examine the image carefully.
        2.  For each attribute, you MUST choose ONLY ONE of the provided options.
        3.  The 'formality' attribute MUST be an integer from 1 (very informal) to 5 (very formal).
        4.  Generate a detailed, engaging, and marketing-oriented description for the item in SPANISH.
        5.  Your response MUST be ONLY the raw JSON object, without any surrounding text, explanations, or markdown like ```json.

        JSON STRUCTURE:
        {
          "attributes": {
            "style": "One of: 'Formal', 'Elegante', 'Casual', 'Deportivo', 'Urbano'",
            "formality": <number>,
            "garmentType": "One of: 'TOP', 'BOTTOM', 'DRESS', 'OUTERWEAR', 'FOOTWEAR', 'ACCESSORY'",
            "colorFamily": "One of: 'Cálido', 'Frío', 'Neutro'",
            "pattern": "One of: 'Liso', 'Rayas', 'Cuadros', 'Floral', 'Animal Print', 'Abstracto'",
            "fit": "One of: 'Ajustado', 'Regular', 'Holgado', 'Oversize'"
          },
          "detailedDescription": "<Your description in SPANISH here>"
        }
        """.formatted(productName);
    }
}