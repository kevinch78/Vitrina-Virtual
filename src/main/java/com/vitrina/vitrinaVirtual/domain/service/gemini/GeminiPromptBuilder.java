package com.vitrina.vitrinaVirtual.domain.service.gemini;

import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GeminiPromptBuilder {

    private static final int MIN_OUTFIT_ITEMS = 3;
    private static final int MAX_OUTFIT_ITEMS = 5;

    public String buildCoherentOutfitPrompt(String gender, String climate, String style, String material, List<ProductWithStoreDto> products) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres un estilista de moda personal y experto. Tu misión es crear un outfit excepcional, coherente y con estilo a partir de una lista de productos disponibles.\n\n");

        // 1. Perfil del Cliente
        prompt.append("### 1. PERFIL DEL CLIENTE\n");
        if (hasValue(gender)) prompt.append("- **Género:** ").append(gender).append("\n");
        if (hasValue(climate)) prompt.append("- **Clima:** ").append(climate).append("\n");
        if (hasValue(style)) prompt.append("- **Estilo deseado:** ").append(style).append("\n");
        if (hasValue(material)) prompt.append("- **Material preferido:** ").append(material).append("\n");
        prompt.append("\n");

        // 2. Reglas de Estilista (CRÍTICAS) - Ahora construidas dinámicamente
        prompt.append("### 2. REGLAS DE ESTILISTA OBLIGATORIAS (Personalizadas para esta petición)\n");
        buildDynamicStylistRules(prompt, gender, climate, style);

        // 3. Productos Disponibles
        prompt.append("### 3. PRODUCTOS DISPONIBLES POR TIPO DE PRENDA\n");
        Map<String, List<ProductWithStoreDto>> categorizedProducts = products.stream()
            .filter(p -> p.getProduct() != null && hasValue(p.getProduct().getGarmentType()))
            .collect(Collectors.groupingBy(p -> p.getProduct().getGarmentType()));

        appendCategoryProducts(prompt, "TOP", categorizedProducts.get("TOP"));
        appendCategoryProducts(prompt, "BOTTOM", categorizedProducts.get("BOTTOM"));
        appendCategoryProducts(prompt, "DRESS", categorizedProducts.get("DRESS"));
        appendCategoryProducts(prompt, "OUTERWEAR", categorizedProducts.get("OUTERWEAR"));
        appendCategoryProducts(prompt, "FOOTWEAR", categorizedProducts.get("FOOTWEAR"));
        appendCategoryProducts(prompt, "ACCESSORY", categorizedProducts.get("ACCESSORY"));
        prompt.append("\n");

        // 4. Instrucciones de la Tarea
        prompt.append("### 4. TU TAREA\n");
        prompt.append("1.  **Crea un outfit completo y con estilo** de ").append(MIN_OUTFIT_ITEMS).append(" a ").append(MAX_OUTFIT_ITEMS).append(" prendas, siguiendo las REGLAS DE ESTILISTA.\n");
        prompt.append("2.  **El outfit DEBE incluir al menos:** `FOOTWEAR` (calzado) y (`TOP` y `BOTTOM`) o un `DRESS`.\n");
        prompt.append("3.  **Si el clima es 'Frío',** es muy recomendable incluir `OUTERWEAR`.\n\n");
        prompt.append("4.  **Selección de Accesorios (MUY IMPORTANTE):**\n");
        prompt.append("    - **Si el outfit es Formal/Elegante,** selecciona un conjunto de hasta 3 accesorios coherentes (ej: 'Corbata de Seda, Reloj Clásico, Cinturón de Cuero').\n");
        prompt.append("    - **Si el outfit es Casual/Deportivo,** selecciona UN accesorio clave que complemente el look (ej: 'Gorra de Béisbol').\n");
        prompt.append("    - Si no encuentras accesorios adecuados en la lista, sugiere uno o varios con el formato `Sugerencia externa: [nombre del accesorio]`.\n");
        prompt.append("5.  **CRÍTICO:** En tu respuesta, usa ÚNICAMENTE los nombres EXACTOS de los productos tal como aparecen en la lista.\n");

        // 5. Formato de Respuesta
        prompt.append("\n### 5. FORMATO DE RESPUESTA (JSON PURO, SIN TEXTO ADICIONAL)\n");
        prompt.append("`json\n`");
        prompt.append("{\n");
        prompt.append("  \"prendas\": [\"< NOMBRE_EXACTO_1>\", \"< NOMBRE_EXACTO_2>\", ...],\n");
        prompt.append("  \"accesorio\": \"<NOMBRE_ACCESORIO_1, NOMBRE_ACCESORIO_2, ... O_SUGERENCIA>\",\n");
        prompt.append("  \"razon\": \"<Breve y convincente explicación de por qué este outfit es una excelente elección de estilismo.>\"\n");
        prompt.append("}\n");
        prompt.append("`");

        return prompt.toString();
    }

    /**
     * Orquesta la construcción de reglas de estilismo dinámicas.
     */
    private void buildDynamicStylistRules(StringBuilder prompt, String gender, String climate, String style) {
        // Reglas universales que siempre aplican
        prompt.append("1.  **Armonía de Color:** Usa la `colorFamily` para crear armonía. Basa el outfit en colores `Neutros` y añade un acento de color `Cálido` o `Frío`, o combina colores de la misma familia.\n");
        prompt.append("2.  **Combinación de Estampados:** ¡CUIDADO! No combines más de UNA prenda con un `pattern` llamativo (ej. `Floral`, `Cuadros`). El resto de prendas deben ser de `pattern: Liso`.\n");
        prompt.append("3.  **Balance de Silueta:** Presta atención al `fit`. Un outfit balanceado suele combinar una prenda `Ajustada` con una más `Holgada` (ej. top ajustado con pantalón holgado), o mantener un `fit: Regular` en todo el conjunto.\n");

        // Reglas específicas que se añaden según el contexto
        int ruleCounter = 4;
        ruleCounter = appendGenderRules(prompt, gender, ruleCounter);
        ruleCounter = appendClimateRules(prompt, climate, ruleCounter);
        appendStyleRules(prompt, style, gender, ruleCounter);

        prompt.append("\n");
    }


    private void appendCategoryProducts(StringBuilder prompt, String categoryName, List<ProductWithStoreDto> products) {
        prompt.append("\n**").append(categoryName).append(":**\n");
        if (products == null || products.isEmpty()) {
            prompt.append("  • [No hay productos disponibles en esta categoría]\n");
            return;
        }
        products.forEach(p -> prompt.append(formatProductForPrompt(p)));
    }

    private String formatProductForPrompt(ProductWithStoreDto p) {
        ProductDto product = p.getProduct();
        if (product == null) return "";

        return String.format("  • %s | Tienda: %s | Color: %s (%s) | Patrón: %s | Corte: %s | Formalidad: %d/5 | Material: %s | Precio: $%.0f\n",
            product.getName(),
            p.getStore().getName(), // Confiamos en que la tienda existe, como bien dijiste.
            nullSafe(product.getPrimaryColor()),
            nullSafe(product.getColorFamily()),
            nullSafe(product.getPattern()),
            nullSafe(product.getFit()),
            product.getFormality() != null ? product.getFormality() : 2, // Default a 2 (casual) si es nulo
            nullSafe(product.getMaterial()),
            product.getPrice() != null ? product.getPrice() : 0.0 // Default a 0.0 si es nulo
        );
    }

    /**
     * Añade reglas específicas para el género.
     */
    private int appendGenderRules(StringBuilder prompt, String gender, int counter) {
        if ("Femenino".equalsIgnoreCase(gender)) {
            prompt.append(counter++).append(".  **Opción de Vestido:** Considera usar una prenda tipo `DRESS` como una alternativa válida a un conjunto de `TOP` y `BOTTOM`.\n");
        }
        return counter;
    }

    /**
     * Añade reglas específicas para el clima.
     */
    private int appendClimateRules(StringBuilder prompt, String climate, int counter) {
        if (!hasValue(climate)) return counter;

        if ("Frío".equalsIgnoreCase(climate)) {
            prompt.append(counter++).append(".  **Regla de Clima Frío:** Es muy recomendable incluir una prenda `OUTERWEAR`. Prioriza materiales como `Lana` y considera añadir una `Bufanda` si está disponible.\n");
        } else if ("Cálido".equalsIgnoreCase(climate)) {
            prompt.append(counter++).append(".  **Regla de Clima Cálido:** **PROHIBIDO** incluir `OUTERWEAR` pesado (Abrigos, Chaquetas de cuero/lana). Prioriza materiales ligeros como `Lino` o `Algodón`.\n");
        }
        return counter;
    }

    /**
     * Añade reglas de estilismo específicas para el estilo y género, enfocándose en calzado y coherencia general.
     * Tiene en cuenta la terminología colombiana ("zapatillas" = tacones).
     */
    private void appendStyleRules(StringBuilder prompt, String style, String gender, int counter) {
        if (!hasValue(style)) return;

        prompt.append(counter).append(".  **Coherencia de Estilo (").append(style).append("):**\n");
        boolean isFeminine = "Femenino".equalsIgnoreCase(gender);

        switch (style.toLowerCase()) {
            case "formal":
            case "elegante":
                prompt.append("    - **Coherencia de Formalidad:** Combina prendas con un nivel de `formalidad` similar (entre 3 y 5). No mezcles con prendas deportivas (`formality` 1).\n");
                if (isFeminine) {
                    prompt.append("    - **Calzado:** El `FOOTWEAR` DEBE ser 'zapatillas' (tacones), 'botines de vestir' o 'sandalias de tacón'. ¡EVITA 'tenis' o calzado deportivo!.\n");
                } else {
                    prompt.append("    - **Calzado:** El `FOOTWEAR` DEBE ser 'zapatos de vestir' o 'mocasines de cuero'. ¡EVITA 'tenis' o calzado deportivo!.\n");
                }
                break;
            case "deportivo":
                prompt.append("    - **Coherencia de Formalidad:** Usa prendas con `formality` baja (1 o 2).\n");
                prompt.append("    - **Calzado:** El `FOOTWEAR` DEBE ser 'tenis' o 'zapatillas deportivas'. ¡EVITA 'zapatos de vestir' o 'zapatillas' (tacones)!.\n");
                break;
            case "casual":
                prompt.append("    - **Coherencia de Formalidad:** Combina prendas con `formality` entre 2 y 4 para un look 'Smart Casual'.\n");
                prompt.append("    - **Calzado:** Tienes libertad. 'Tenis', 'botas casuales', 'sandalias' o 'mocasines' son buenas opciones.\n");
                break;
            default:
                // No añadir regla si el estilo no es uno de los principales
                break;
        }
        prompt.append("\n");
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String nullSafe(String value) {
        return value == null ? "N/A" : value;
    }
}
