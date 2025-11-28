package com.vitrina.vitrinaVirtual.domain.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vitrina.vitrinaVirtual.domain.dto.ProductAnalysisDto;
import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;
import com.vitrina.vitrinaVirtual.domain.dto.OutfitRecommendation; // Corregido para que apunte a la clase correcta
import com.vitrina.vitrinaVirtual.domain.repository.ProductRepository;
import com.vitrina.vitrinaVirtual.domain.service.gemini.ReplicateImageService;

@Service
public class ProductServiceImpl implements ProductService {
    
    private static final Log logger = LogFactory.getLog(ProductServiceImpl.class);
    
    @Autowired private ProductRepository productRepository;
    @Autowired private StoreService storeService;
    @Autowired private GeminiService geminiService;
    // @Autowired private OutfitImageService outfitImageService;
    @Autowired private ReplicateImageService replicateImageService; // NUEVO


    private static final int MAX_PRODUCTS_FOR_AI = 60; // Aumentado según nuestra charla
    private static final int MIN_PRODUCTS_FOR_OUTFIT = 4;

    // Cache para stores activos
    private Set<Long> cachedActiveStoreIds = null;
    private long lastStoreRefresh = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutos

    private static final Map<String, List<String>> STYLE_COMPATIBILITY_MAP = Map.of(
        "Formal", List.of("Elegante", "Tradicional", "Clásico"),
        "Casual", List.of("Urbano", "Relajado", "Básico"),
        "Deportivo", List.of("Athleisure", "Cómodo", "Urbano"),
        "Tradicional", List.of("Formal", "Clásico", "Elegante")
    );

    private static final Map<String, String> CLIMATE_KEYWORDS = Map.ofEntries(
        Map.entry("frío", "Frío"), Map.entry("frio", "Frío"), Map.entry("invierno", "Frío"), Map.entry("fresca", "Frío"),
        Map.entry("templado", "Templado"), Map.entry("primavera", "Templado"), Map.entry("otoño", "Templado"), Map.entry("moderado", "Templado")
    );

    private static final Map<String, String> STYLE_KEYWORDS = Map.ofEntries(
        Map.entry("formal", "Formal"), Map.entry("elegante", "Formal"), Map.entry("oficina", "Formal"), Map.entry("trabajo", "Formal"), Map.entry("boda", "Formal"), Map.entry("ceremonia", "Formal"), Map.entry("gala", "Formal"), Map.entry("fiesta", "Formal"), Map.entry("noche", "Formal"), Map.entry("cena", "Formal"),
        Map.entry("casual", "Casual"), Map.entry("informal", "Casual"), Map.entry("relajado", "Casual"), Map.entry("playa", "Casual"), Map.entry("piscina", "Casual"), Map.entry("casa", "Casual"),
        Map.entry("deportivo", "Deportivo"), Map.entry("sport", "Deportivo"), Map.entry("gimnasio", "Deportivo"), Map.entry("ejercicio", "Deportivo"),
        Map.entry("tradicional", "Tradicional"), Map.entry("clásico", "Tradicional"), Map.entry("conservador", "Tradicional")
    );

    private static final Map<String, String> MATERIAL_KEYWORDS = Map.of(
        "lana", "lana", "seda", "seda", "algodón", "algodón",
        "algodon", "algodón", "cuero", "cuero", "denim", "denim", "jean", "denim", "lino", "lino"
    );

    private String findFirstMatch(String text, Map<String, String> keywordMap) {
        return keywordMap.entrySet().stream().filter(entry -> text.contains(entry.getKey())).map(Map.Entry::getValue).findFirst().orElse(null);
    }

    @Override
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public ProductDto getProductById(Long productId) {
        return productRepository.findById(productId);
    }

    @Override
    public ProductDto saveProduct(ProductDto productDto, String imageBase64) {
        // 1. Enriquecimiento con IA (si hay imagen)
        if (hasValue(imageBase64)) {
            try {
                logger.info(String.format("Iniciando análisis con IA para el producto: '%s'", productDto.getName()));
                ProductAnalysisDto analysis = geminiService.analyzeProductImage(imageBase64, productDto.getName());
                applyAiAnalysisToDto(productDto, analysis);
                logger.info(String.format("Análisis de IA aplicado exitosamente para el producto: '%s'", productDto.getName()));
            } catch (Exception e) {
                // Si la IA falla, no detenemos la creación del producto.
                // Logueamos el error y continuamos. El enriquecimiento manual actuará como fallback.
                logger.error("Falló el análisis con IA para el producto '" + productDto.getName() + "'. Se procederá con el enriquecimiento manual.", e);
            }
        }

        // 2. Enriquecimiento manual (actúa como fallback o para productos sin imagen)
        // Este método ahora solo rellenará los campos que la IA no haya podido completar.
        enrichProductDtoManually(productDto);

        // 3. Guardar el producto enriquecido
        return productRepository.save(productDto);
    }

    @Override
    public void deleteProductById(Long productId) {
        productRepository.deleteById(productId);
    }

    @Override
    public List<ProductDto> getProductsByStyle(String style) {
        return productRepository.findByStyle(style);
    }

    @Override
    public List<ProductDto> getProductsByStoreId(Long storeId) {
        return productRepository.findByStoreId(storeId);
    }

    @Override
    public List<ProductDto> getRecommendedProducts(List<Long> storeIds, String gender, String climate, String style) {
        return productRepository.findByRecommendedProducts(storeIds, gender, climate, style);
    }

    @Override
    public List<ProductWithStoreDto> getProductsWithStores(List<Long> storeIds, String gender, String climate, String style) {
        Set<Long> activeStoreIds = getActiveAdvertisingStoreIds();
        if (storeIds != null && !storeIds.isEmpty()) {
            activeStoreIds.retainAll(new HashSet<>(storeIds));
        }
        if (activeStoreIds.isEmpty()) {
            logger.warn("No active stores found");
            return Collections.emptyList();
        }
        
        logger.info(String.format("Active stores: %d | Filters - Gender: %s, Climate: %s, Style: %s", activeStoreIds.size(), gender, climate, style));
        
        List<ProductDto> products = getFilteredProductsWithFallback(activeStoreIds, gender, climate, style);
        logger.info(String.format("Products found after filtering: %d", products.size()));
        
        return mapAndBalanceProducts(products, activeStoreIds);
    }

    @Override
    public OutfitRecommendation generateOutfit(List<Long> storeIds, String gender, String climate, String style, String material, boolean generateImage) {
        logger.info(String.format("Generating outfit with filters: gender=%s, climate=%s, style=%s, material=%s", gender, climate, style, material));
        List<ProductWithStoreDto> filteredProducts = getProductsWithStores(storeIds, gender, climate, style);

        if (filteredProducts.isEmpty()) {
            return new OutfitRecommendation(Collections.emptyList(), "No hay productos disponibles con los filtros aplicados");
        }

        if (hasValue(material)) {
            List<ProductWithStoreDto> materialFiltered = filteredProducts.stream()
                    .filter(p -> productMatchesMaterial(p.getProduct(), material))
                    .collect(Collectors.toList());
            if (materialFiltered.size() >= MIN_PRODUCTS_FOR_OUTFIT) {
                filteredProducts = materialFiltered;
            }
        }

        filteredProducts = optimizeProductSelectionForOutfit(filteredProducts, climate);
        OutfitRecommendation outfit = geminiService.getOutfitRecommendation(gender, climate, style, material, filteredProducts);

        return generateAndAttachOutfitImage(outfit, gender, climate, style, generateImage);
    }

    @Override
    public OutfitRecommendation generateOutfitFromChat(String message, String gender, boolean generateImage) {
        logger.info("[CHAT_GEN] === CHAT DEBUG START ===");
        logger.info(String.format("[CHAT_GEN] Original message: '%s'", message));
        logger.info(String.format("[CHAT_GEN] Original gender: '%s'", gender));
        
        // Normalizar gender para que coincida con BD
        gender = normalizeGender(gender);
        logger.info(String.format("[CHAT_GEN] Normalized gender: '%s'", gender));
        
        ChatFilters filters = parseMessageFilters(message);
        filters.gender = gender;
        
        logger.info(String.format("[CHAT_GEN] Parsed filters - Gender: '%s', Climate: '%s', Style: '%s', Material: '%s'", filters.gender, filters.climate, filters.style, filters.material));

        // PASO 1, 2 y 3: Cargar, enriquecer y filtrar de forma estricta.
        List<ProductWithStoreDto> products = getProductsWithStores(null, filters.gender, filters.climate, filters.style);
        logger.info(String.format("[CHAT_GEN] Products found after initial filter: %d", products.size()));

        products = applyAdditionalChatFilters(products, filters);
        logger.info(String.format("[CHAT_GEN] Products after additional filters: %d", products.size()));

        // Si después de los filtros estrictos no hay suficientes productos, no hay nada que enviar a la IA.
        // Dejamos que GeminiService active su fallback.
        if (products.isEmpty()) {
            logger.warn("Not enough products after strict filtering. Triggering fallback.");
            // La lista (aunque pequeña o vacía) se pasa para que el fallback decida.
        }

        products = optimizeProductSelectionForOutfit(products, filters.climate);
        logger.info(String.format("[CHAT_GEN] Products after optimization: %d", products.size()));
        logger.info("[CHAT_GEN] === CHAT DEBUG END ===");
        
        OutfitRecommendation outfit = geminiService.getOutfitRecommendation(filters.gender, filters.climate, filters.style, filters.material, products);
        return generateAndAttachOutfitImage(outfit, gender, filters.climate, filters.style, generateImage);
    }

    /**
     * Orquesta la generación de la imagen fotorrealista y la adjunta al DTO de recomendación.
     * @param outfit La recomendación de outfit generada por la IA Estilista.
     * @param gender El género para seleccionar el modelo.
     * @param climate El clima para el contexto de la imagen.
     * @param style El estilo para el contexto de la imagen.
     * @param generateImage Si es true, se generará la imagen.
     * @return El DTO de recomendación, enriquecido con la URL de la imagen si se solicitó.
     */
    private OutfitRecommendation generateAndAttachOutfitImage(OutfitRecommendation outfit, String gender, String climate, String style, boolean generateImage) {
        if (generateImage && outfit != null && outfit.getSelectedProducts() != null && !outfit.getSelectedProducts().isEmpty()) {
            try {
                logger.info("Usuario solicitó generación de imagen. Iniciando proceso...");
                String imageUrl = replicateImageService.generateOutfitImage(outfit, gender, climate, style);
                outfit.setImageUrl(imageUrl);
                logger.info("Imagen generada y añadida al outfit: " + imageUrl);
            } catch (Exception e) {
                logger.warn("No se pudo generar la imagen del outfit, continuando sin ella.", e);
                outfit.setImageUrl(null); // Asegurarse de que la URL sea nula si falla.
            }
        }
        return outfit;
    }

    // ============ MÉTODOS PRIVADOS DE LÓGICA DE PRODUCTOS ============
    
    private String normalizeGender(String gender) {
        if (!hasValue(gender)) return null;
        String normalized = gender.trim().toLowerCase();
        
        if (normalized.contains("masc") || normalized.equals("m")) {
            return "Masculino";
        } else if (normalized.contains("fem") || normalized.equals("f")) {
            return "Femenino";
        }
        
        // Si ya viene capitalizado correctamente, devolverlo
        if ("Masculino".equals(gender) || "Femenino".equals(gender)) {
            return gender;
        }
        
        return "Masculino"; // Default
    }

    private Set<Long> getActiveAdvertisingStoreIds() {
        long now = System.currentTimeMillis();
        if (cachedActiveStoreIds == null || (now - lastStoreRefresh) > CACHE_DURATION) {
            cachedActiveStoreIds = storeService.getAllStores().stream()
                    .filter(store -> Boolean.TRUE.equals(store.getActiveAdvertising()))
                    .map(StoreDto::getStoreId)
                    .collect(Collectors.toSet());
            lastStoreRefresh = now;
            logger.debug(String.format("Refreshed active store cache: %d stores", cachedActiveStoreIds.size()));
        }
        return new HashSet<>(cachedActiveStoreIds);
    }

    private List<ProductDto> getFilteredProductsWithFallback(Set<Long> activeStoreIds, String gender, String climate, String style) {
        // 1. Obtener todos los productos de las tiendas activas. Es más eficiente hacer una sola llamada a la BD.
        List<ProductDto> allProducts = productRepository.findByStoreIdIn(new ArrayList<>(activeStoreIds));
        logger.info(String.format("Total products from active stores: %d", allProducts.size()));

        // --- LÓGICA DE BÚSQUEDA EN CASCADA ---

        // Intento 1: Búsqueda estricta (ideal)
        List<ProductDto> filtered = allProducts.stream()
            .filter(p -> hasValue(gender) ? gender.equalsIgnoreCase(p.getGender()) : true)
            .filter(p -> hasValue(climate) ? climate.equalsIgnoreCase(p.getClimate()) : true)
            .filter(p -> hasValue(style) ? style.equalsIgnoreCase(p.getStyle()) : true)
            .collect(Collectors.toList());
        
        if (filtered.size() >= MIN_PRODUCTS_FOR_OUTFIT) {
            logger.info(String.format("Found %d products with strict filtering.", filtered.size()));
            return filtered;
        }
        logger.warn(String.format("Strict search found only %d products. Trying flexible style search...", filtered.size()));

        // Intento 2: Flexibilizar el estilo (manteniendo género y clima estrictos)
        List<String> compatibleStyles = new ArrayList<>();
        if (hasValue(style)) {
            compatibleStyles.add(style);
            compatibleStyles.addAll(STYLE_COMPATIBILITY_MAP.getOrDefault(style, Collections.emptyList()));
        }

        if (!compatibleStyles.isEmpty()) {
            filtered = allProducts.stream()
                .filter(p -> hasValue(gender) ? gender.equalsIgnoreCase(p.getGender()) : true)
                .filter(p -> hasValue(climate) ? climate.equalsIgnoreCase(p.getClimate()) : true)
                .filter(p -> compatibleStyles.contains(p.getStyle()))
                .collect(Collectors.toList());

            if (filtered.size() >= MIN_PRODUCTS_FOR_OUTFIT) {
                logger.info(String.format("Found %d products with flexible style filtering.", filtered.size()));
                return filtered;
            }
        }
        logger.warn(String.format("Flexible style search found only %d products. Returning this result.", filtered.size()));
        return filtered; // Devolvemos lo que se haya encontrado, aunque sea poco.
    }

    private List<ProductWithStoreDto> mapAndBalanceProducts(List<ProductDto> products, Set<Long> activeStoreIds) {
        Map<Long, StoreDto> storeMap = storeService.getAllStores().stream()
                .filter(s -> activeStoreIds.contains(s.getStoreId()))
                .collect(Collectors.toMap(StoreDto::getStoreId, Function.identity()));

        // !! CORRECCIÓN CRÍTICA !!
        // El enriquecimiento debe ocurrir aquí, ANTES de cualquier filtrado.
        products.forEach(this::enrichProductDtoManually);

        List<ProductWithStoreDto> mapped = products.stream()
                .filter(p -> p.getStoreId() != null && storeMap.containsKey(p.getStoreId()))
                .map(p -> new ProductWithStoreDto(p, storeMap.get(p.getStoreId())))
                .collect(Collectors.toList());


        Set<Long> seenProducts = new HashSet<>();
        List<ProductWithStoreDto> deduplicated = mapped.stream()
                .filter(p -> seenProducts.add(p.getProduct().getIdProduct()))
                .collect(Collectors.toList());
                
        logger.info(String.format("Mapped %d products to %d unique products", mapped.size(), deduplicated.size()));
        return deduplicated;
    }

    private List<ProductWithStoreDto> optimizeProductSelectionForOutfit(List<ProductWithStoreDto> products, String climate) {
        if (products.size() <= MAX_PRODUCTS_FOR_AI) {
            return products;
        }

        // 1. Definir categorías esenciales basadas en el clima (¡TU IDEA!)
        Set<String> essentialCategories = new HashSet<>(Set.of("TOP", "BOTTOM", "FOOTWEAR"));
        if (hasValue(climate) && (climate.equalsIgnoreCase("Frío") || climate.equalsIgnoreCase("Templado"))) {
            essentialCategories.add("OUTERWEAR");
        }

        // 2. Agrupar todos los productos por su tipo de prenda.
        Map<String, List<ProductWithStoreDto>> categorizedProducts = products.stream()
            .filter(p -> hasValue(p.getProduct().getGarmentType()))
            .collect(Collectors.groupingBy(p -> p.getProduct().getGarmentType()));

        List<ProductWithStoreDto> finalSelection = new ArrayList<>();
        List<ProductWithStoreDto> remainingProducts = new ArrayList<>();

        // 3. Garantizar la inclusión de prendas esenciales.
        for (String category : essentialCategories) {
            if (categorizedProducts.containsKey(category)) {
                List<ProductWithStoreDto> categoryList = categorizedProducts.get(category);
                Collections.shuffle(categoryList);
                // Tomamos hasta 5 de cada categoría esencial para dar opciones.
                int limit = Math.min(categoryList.size(), 5);
                finalSelection.addAll(categoryList.subList(0, limit));
            }
        }

        // 4. Recolectar el resto de productos (no esenciales y los que sobraron de los esenciales).
        Set<ProductWithStoreDto> alreadySelected = new HashSet<>(finalSelection);
        for (Map.Entry<String, List<ProductWithStoreDto>> entry : categorizedProducts.entrySet()) {
            entry.getValue().stream()
                .filter(p -> !alreadySelected.contains(p))
                .forEach(remainingProducts::add);
        }

        // 5. Rellenar el espacio restante con el resto de productos de forma aleatoria.
        Collections.shuffle(remainingProducts);
        int spaceToFill = MAX_PRODUCTS_FOR_AI - finalSelection.size();
        if (spaceToFill > 0) {
            finalSelection.addAll(remainingProducts.stream().limit(spaceToFill).collect(Collectors.toList()));
        }

        // 6. Si la selección final excede el límite (porque los esenciales ya eran muchos),
        // se recorta de forma aleatoria. Esto es una salvaguarda.
        if (finalSelection.size() > MAX_PRODUCTS_FOR_AI) {
            Collections.shuffle(finalSelection);
            return finalSelection.subList(0, MAX_PRODUCTS_FOR_AI);
        }

        return finalSelection;
    }

    private void applyAiAnalysisToDto(ProductDto productDto, ProductAnalysisDto analysis) {
        if (analysis == null) {
            logger.warn("El objeto de análisis de IA es nulo. No se aplicarán los atributos de la IA.");
            return;
        }

        // Asignación de la descripción de la IA usando Optional para un manejo de nulos limpio.
        // Si la descripción existe y no está vacía, se asigna.
        Optional.ofNullable(analysis.getDetailedDescription())
                .filter(ProductServiceImpl::hasValue) // Reutilizamos nuestro método hasValue
                .ifPresent(productDto::setIaDescription);

        // Asignación de la descripción TÉCNICA de la IA.
        Optional.ofNullable(analysis.getTechnicalDescription())
                .filter(ProductServiceImpl::hasValue)
                .ifPresent(productDto::setTechnicalDescription);

        // Asignación de atributos.
        Optional.ofNullable(analysis.getAttributes()).ifPresent(attributes -> {
            productDto.setStyle(attributes.getStyle());
            productDto.setFormality(attributes.getFormality());
            productDto.setGarmentType(attributes.getGarmentType());
            productDto.setColorFamily(attributes.getColorFamily());
            productDto.setPattern(attributes.getPattern());
            productDto.setFit(attributes.getFit());
        });
    }

    /**
     * Enriquece un ProductDto con atributos derivados manualmente.
     * Este método actúa como un fallback si el análisis de la IA falla o no se ejecuta.
     * Solo establece valores si no han sido ya establecidos por la IA.
     * @param product El DTO del producto a enriquecer.
     */
    private void enrichProductDtoManually(ProductDto product) {
        if (product == null) return;
        // Lógica de fallback para rellenar campos si están vacíos
        if (!hasValue(product.getGarmentType())) product.setGarmentType(determineGarmentTypeManually(product));
        if (!hasValue(product.getClimate())) product.setClimate(determineClimateManually(product));
        if (!hasValue(product.getOccasion())) product.setOccasion(determineOccasionManually(product));
        if (!hasValue(product.getColorFamily())) product.setColorFamily(determineColorFamilyManually(product.getPrimaryColor()));
    }

    private String determineGarmentTypeManually(ProductDto product) {
        String text = (nullSafe(product.getSubcategory()) + " " + nullSafe(product.getName())).toLowerCase();

        if (containsAny(text, new String[]{"vestido", "enterizo", "jumpsuit", "romper"})) return "DRESS";
        if (containsAny(text, new String[]{"abrigo", "chaqueta", "saco", "blazer", "chamarra", "cardigan", "gabardina", "parka"})) return "OUTERWEAR";
        if (containsAny(text, new String[]{"camiseta", "camisa", "blusa", "top", "polo", "sueter", "jersey", "sudadera", "hoodie"})) return "TOP";
        if (containsAny(text, new String[]{"pantalon", "pantalón", "short", "falda", "jean", "bermuda", "legging", "jogger"})) return "BOTTOM";
        if (containsAny(text, new String[]{"calzado", "zapato", "bota", "sandalia", "tenis", "mocasin", "zapatilla", "tacon", "tacón"})) return "FOOTWEAR";
        if (containsAny(text, new String[]{"accesorio", "bolso", "cinturon", "cinturón", "collar", "reloj", "bufanda", "gorra", "sombrero", "gafas", "lentes"})) return "ACCESSORY";
        
        return "OTHER";
    }

    private String determineClimateManually(ProductDto product) {
        String garmentType = product.getGarmentType();
        if (garmentType == null) return "Templado"; // Default

        switch (garmentType.toUpperCase()) {
            case "OUTERWEAR":
                return "Frío";
            case "DRESS":
            case "BOTTOM":
            case "TOP":
                return "Templado";
            default:
                return "Cálido";
        }
    }

    private String determineOccasionManually(ProductDto product) {
        Integer formality = product.getFormality();
        if (formality == null) return "Casual"; // Default
        return formality >= 3 ? "Formal" : "Casual";
    }

    private String determineColorFamilyManually(String color) {
        if (!hasValue(color)) return "Neutro";
        String c = color.toLowerCase();

        if (containsAny(c, new String[]{"rojo", "naranja", "amarillo", "rosado", "fucsia", "terracota"})) return "Cálido";
        if (containsAny(c, new String[]{"azul", "verde", "violeta", "morado", "turquesa"})) return "Frío";
        if (containsAny(c, new String[]{"blanco", "negro", "gris", "beige", "marron", "marrón", "crema"})) return "Neutro";

        return "Neutro";
    }

    private boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private boolean productMatchesMaterial(ProductDto product, String requestedMaterial) {
        if (product.getMaterial() == null || requestedMaterial == null) return false;
        return product.getMaterial().toLowerCase().contains(requestedMaterial.toLowerCase());
    }

    // ============ PARSEADOR DE MENSAJES CORREGIDO ============
    
    private ChatFilters parseMessageFilters(String message) {
        if (message == null) return new ChatFilters();
        String msg = message.toLowerCase(Locale.ROOT);
        ChatFilters filters = new ChatFilters();

        // MEJORA: Usar mapas para un parseo más limpio y extensible
        filters.climate = findFirstMatch(msg, CLIMATE_KEYWORDS);
        filters.style = findFirstMatch(msg, STYLE_KEYWORDS);
        filters.material = findFirstMatch(msg, MATERIAL_KEYWORDS);

        filters.aspirational = msg.contains("premium") || msg.contains("lujo") || msg.contains("exclusivo");
        filters.minimalist = msg.contains("minimalista") || msg.contains("básico") || msg.contains("basico");

        logger.info(String.format("Parsed message '%s' -> Climate: '%s', Style: '%s', Material: '%s'", message, filters.climate, filters.style, filters.material));

        return filters;
    }

    private List<ProductWithStoreDto> applyAdditionalChatFilters(List<ProductWithStoreDto> products, ChatFilters filters) {
        List<ProductWithStoreDto> filtered = new ArrayList<>(products);
        
        if (hasValue(filters.material)) {
            List<ProductWithStoreDto> materialFiltered = filtered.stream()
                .filter(p -> productMatchesMaterial(p.getProduct(), filters.material))
                .collect(Collectors.toList());
            if (materialFiltered.size() >= MIN_PRODUCTS_FOR_OUTFIT) {
                filtered = materialFiltered;
                logger.info(String.format("Applied material filter '%s': %d products", filters.material, filtered.size()));
            }
        }
        
        if (filters.aspirational && filtered.size() > MIN_PRODUCTS_FOR_OUTFIT * 2) {
            double avgPrice = filtered.stream().mapToDouble(p -> p.getProduct().getPrice()).average().orElse(0);
            List<ProductWithStoreDto> premiumFiltered = filtered.stream()
                .filter(p -> p.getProduct().getPrice() >= avgPrice)
                .collect(Collectors.toList());
            if (premiumFiltered.size() >= MIN_PRODUCTS_FOR_OUTFIT) {
                filtered = premiumFiltered;
                logger.info(String.format("Applied premium filter (price >= %.2f): %d products", avgPrice, filtered.size()));
            }
        }
        
        if (filters.minimalist && filtered.size() > MIN_PRODUCTS_FOR_OUTFIT * 3) {
            Collections.shuffle(filtered);
            filtered = filtered.stream().limit(MIN_PRODUCTS_FOR_OUTFIT * 2).collect(Collectors.toList());
            logger.info(String.format("Applied minimalist filter: %d products", filtered.size()));
        }
        
        return filtered;
    }

    // ============ MÉTODOS AUXILIARES ============
    
    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static class ChatFilters {
        String gender, climate, style, material;
        boolean aspirational = false, minimalist = false;
    }
}
