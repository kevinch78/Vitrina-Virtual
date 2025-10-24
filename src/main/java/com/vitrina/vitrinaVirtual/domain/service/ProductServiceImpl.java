package com.vitrina.vitrinaVirtual.domain.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;
import com.vitrina.vitrinaVirtual.domain.dto.OutfitRecommendation;
import com.vitrina.vitrinaVirtual.domain.repository.ProductRepository;

@Service
public class ProductServiceImpl implements ProductService {
    
    private static final Log logger = LogFactory.getLog(ProductServiceImpl.class);
    
    @Autowired private ProductRepository productRepository;
    @Autowired private StoreService storeService;
    @Autowired private GeminiService geminiService;

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
    public ProductDto saveProduct(ProductDto productDto) {
        enrichProductDto(productDto);
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
        
        logger.info("Active stores: " + activeStoreIds.size() + " | Filters - Gender: " + gender + ", Climate: " + climate + ", Style: " + style);
        
        List<ProductDto> products = getFilteredProductsWithFallback(activeStoreIds, gender, climate, style);
        logger.info("Products found after filtering: " + products.size());
        
        return mapAndBalanceProducts(products, activeStoreIds);
    }

    @Override
    public OutfitRecommendation generateOutfit(List<Long> storeIds, String gender, String climate, String style, String material) {
        logger.info("Generating outfit with filters: gender=" + gender + ", climate=" + climate + ", style=" + style + ", material=" + material);
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
        return geminiService.getOutfitRecommendation(gender, climate, style, material, filteredProducts);
    }

    @Override
    public OutfitRecommendation generateOutfitFromChat(String message, String gender) {
        logger.info("=== CHAT DEBUG START ===");
        logger.info("Original message: '" + message + "'");
        logger.info("Original gender: '" + gender + "'");
        
        // Normalizar gender para que coincida con BD
        gender = normalizeGender(gender);
        logger.info("Normalized gender: '" + gender + "'");
        
        ChatFilters filters = parseMessageFilters(message);
        filters.gender = gender;
        
        logger.info("Parsed filters - Gender: '" + filters.gender + "', Climate: '" + filters.climate + "', Style: '" + filters.style + "', Material: '" + filters.material + "'");

        // PASO 1, 2 y 3: Cargar, enriquecer y filtrar de forma estricta.
        List<ProductWithStoreDto> products = getProductsWithStores(null, filters.gender, filters.climate, filters.style);
        logger.info("Products found after initial filter: " + products.size());

        products = applyAdditionalChatFilters(products, filters);
        logger.info("Products after additional filters: " + products.size());

        // Si después de los filtros estrictos no hay suficientes productos, no hay nada que enviar a la IA.
        // Dejamos que GeminiService active su fallback.
        if (products.size() < MIN_PRODUCTS_FOR_OUTFIT) {
            logger.warn("Not enough products after strict filtering (" + products.size() + "). Triggering fallback.");
            // La lista (aunque pequeña o vacía) se pasa para que el fallback decida.
        }

        products = optimizeProductSelectionForOutfit(products, filters.climate);
        logger.info("Products after optimization: " + products.size());
        logger.info("=== CHAT DEBUG END ===");
        
        return geminiService.getOutfitRecommendation(filters.gender, filters.climate, filters.style, filters.material, products);
    }

    // ============ MÉTODOS PRIVADOS CORREGIDOS ============
    
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
            logger.debug("Refreshed active store cache: " + cachedActiveStoreIds.size() + " stores");
        }
        return new HashSet<>(cachedActiveStoreIds);
    }

    private List<ProductDto> getFilteredProductsWithFallback(Set<Long> activeStoreIds, String gender, String climate, String style) {
        // 1. Obtener todos los productos de las tiendas activas. Es más eficiente hacer una sola llamada a la BD.
        List<ProductDto> allProducts = productRepository.findByStoreIdIn(new ArrayList<>(activeStoreIds));
        logger.info("Total products from active stores: " + allProducts.size());

        // --- LÓGICA DE BÚSQUEDA EN CASCADA ---

        // Intento 1: Búsqueda estricta (ideal)
        List<ProductDto> filtered = allProducts.stream()
            .filter(p -> hasValue(gender) ? gender.equalsIgnoreCase(p.getGender()) : true)
            .filter(p -> hasValue(climate) ? climate.equalsIgnoreCase(p.getClimate()) : true)
            .filter(p -> hasValue(style) ? style.equalsIgnoreCase(p.getStyle()) : true)
            .collect(Collectors.toList());
        
        if (filtered.size() >= MIN_PRODUCTS_FOR_OUTFIT) {
            logger.info("Found " + filtered.size() + " products with strict filtering.");
            return filtered;
        }
        logger.warn("Strict search found only " + filtered.size() + " products. Trying flexible style search...");

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
                logger.info("Found " + filtered.size() + " products with flexible style filtering.");
                return filtered;
            }
        }
        logger.warn("Flexible style search found only " + filtered.size() + " products. Returning this result.");
        return filtered; // Devolvemos lo que se haya encontrado, aunque sea poco.
    }

    private List<ProductWithStoreDto> mapAndBalanceProducts(List<ProductDto> products, Set<Long> activeStoreIds) {
        Map<Long, StoreDto> storeMap = storeService.getAllStores().stream()
                .filter(s -> activeStoreIds.contains(s.getStoreId()))
                .collect(Collectors.toMap(StoreDto::getStoreId, Function.identity()));

        // !! CORRECCIÓN CRÍTICA !!
        // El enriquecimiento debe ocurrir aquí, ANTES de cualquier filtrado.
        products.forEach(this::enrichProductDto);

        List<ProductWithStoreDto> mapped = products.stream()
                .filter(p -> p.getStoreId() != null && storeMap.containsKey(p.getStoreId()))
                .map(p -> new ProductWithStoreDto(p, storeMap.get(p.getStoreId())))
                .collect(Collectors.toList());


        Set<Long> seenProducts = new HashSet<>();
        List<ProductWithStoreDto> deduplicated = mapped.stream()
                .filter(p -> seenProducts.add(p.getProduct().getIdProduct()))
                .collect(Collectors.toList());
                
        logger.info("Mapped " + mapped.size() + " products to " + deduplicated.size() + " unique products");
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

    private void enrichProductDto(ProductDto product) {
        if (product == null) return;
        product.setGarmentType(determineGarmentType(product));
        product.setColorFamily(determineColorFamily(product.getPrimaryColor()));
    }

    private String determineGarmentType(ProductDto product) {
        String text = (nullSafe(product.getSubcategory()) + " " + nullSafe(product.getName())).toLowerCase();

        if (containsAny(text, new String[]{"vestido", "enterizo", "jumpsuit", "romper"})) return "DRESS";
        if (containsAny(text, new String[]{"abrigo", "chaqueta", "saco", "blazer", "chamarra", "cardigan", "gabardina", "parka"})) return "OUTERWEAR";
        if (containsAny(text, new String[]{"camiseta", "camisa", "blusa", "top", "polo", "sueter", "jersey", "sudadera", "hoodie"})) return "TOP";
        if (containsAny(text, new String[]{"pantalon", "pantalón", "short", "falda", "jean", "bermuda", "legging", "jogger"})) return "BOTTOM";
        if (containsAny(text, new String[]{"calzado", "zapato", "bota", "sandalia", "tenis", "mocasin", "zapatilla", "tacon", "tacón"})) return "FOOTWEAR";
        if (containsAny(text, new String[]{"accesorio", "bolso", "cinturon", "cinturón", "collar", "reloj", "bufanda", "gorra", "sombrero", "gafas", "lentes"})) return "ACCESSORY";
        
        return "OTHER";
    }

    private String determineColorFamily(String color) {
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

        logger.info("Parsed message '" + message + "' -> Climate: '" + filters.climate + "', Style: '" + filters.style + "', Material: '" + filters.material + "'");

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
                logger.info("Applied material filter '" + filters.material + "': " + filtered.size() + " products");
            }
        }
        
        if (filters.aspirational && filtered.size() > MIN_PRODUCTS_FOR_OUTFIT * 2) {
            double avgPrice = filtered.stream().mapToDouble(p -> p.getProduct().getPrice()).average().orElse(0);
            List<ProductWithStoreDto> premiumFiltered = filtered.stream()
                .filter(p -> p.getProduct().getPrice() >= avgPrice)
                .collect(Collectors.toList());
            if (premiumFiltered.size() >= MIN_PRODUCTS_FOR_OUTFIT) {
                filtered = premiumFiltered;
                logger.info("Applied premium filter (price >= " + avgPrice + "): " + filtered.size() + " products");
            }
        }
        
        if (filters.minimalist && filtered.size() > MIN_PRODUCTS_FOR_OUTFIT * 3) {
            Collections.shuffle(filtered);
            filtered = filtered.stream().limit(MIN_PRODUCTS_FOR_OUTFIT * 2).collect(Collectors.toList());
            logger.info("Applied minimalist filter: " + filtered.size() + " products");
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
// package com.vitrina.vitrinaVirtual.domain.service;

// import java.util.*;
// import java.util.function.Function;
// import java.util.stream.Collectors;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;

// import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
// import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
// import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;
// import com.vitrina.vitrinaVirtual.domain.dto.OutfitRecommendation;
// import com.vitrina.vitrinaVirtual.domain.repository.ProductRepository;

// @Service
// public class ProductServiceImpl implements ProductService {
    
//     private static final Log logger = LogFactory.getLog(ProductServiceImpl.class);
    
//     @Autowired private ProductRepository productRepository;
//     @Autowired private StoreService storeService;
//     @Autowired private GeminiService geminiService;

//     private static final int MAX_PRODUCTS_FOR_AI = 40; // Aumentamos ligeramente para dar más opciones a la IA
//     private static final int MIN_PRODUCTS_FOR_OUTFIT = 4;
//     private static final int PRODUCTS_PER_CATEGORY = 10; // Aumentamos para tener más variedad por categoría

//     private static final Map<String, List<String>> STYLE_COMPATIBILITY_MAP = Map.of(
//         "formal", List.of("elegante", "minimalista", "clásico", "business casual"),
//         "casual", List.of("urbano", "relajado", "básico", "streetwear"),
//         "deportivo", List.of("athleisure", "cómodo", "urbano"),
//         "urbano", List.of("casual", "deportivo", "streetwear"),
//         "elegante", List.of("formal", "sofisticado", "minimalista"),
//         "bohemio", List.of("relajado", "artesanal", "hippie", "folk")
//     );

//     @Override
//     public List<ProductDto> getAllProducts() {
//         return productRepository.findAll();
//     }

//     @Override
//     public ProductDto getProductById(Long productId) {
//         return productRepository.findById(productId);
//     }

//     @Override
//     public ProductDto saveProduct(ProductDto productDto) {
//         // Centralizamos el enriquecimiento del producto aquí
//         enrichProductDto(productDto);
//         return productRepository.save(productDto);
//     }

//     @Override
//     public void deleteProductById(Long productId) {
//         productRepository.deleteById(productId);
//     }

//     @Override
//     public List<ProductDto> getProductsByStyle(String style) {
//         return productRepository.findByStyle(style);
//     }

//     @Override
//     public List<ProductDto> getProductsByStoreId(Long storeId) {
//         return productRepository.findByStoreId(storeId);
//     }

//     @Override
//     public List<ProductDto> getRecommendedProducts(List<Long> storeIds, String gender, String climate, String style) {
//         return productRepository.findByRecommendedProducts(storeIds, gender, climate, style);
//     }

//     @Override
//     public List<ProductWithStoreDto> getProductsWithStores(List<Long> storeIds, String gender, String climate, String style) {
//         Set<Long> activeStoreIds = getActiveAdvertisingStoreIds();
//         if (storeIds != null && !storeIds.isEmpty()) {
//             activeStoreIds.retainAll(new HashSet<>(storeIds));
//         }
//         if (activeStoreIds.isEmpty()) {
//             return Collections.emptyList();
//         }
//         List<ProductDto> products = getFilteredProductsWithFallback(activeStoreIds, gender, climate, style);
//         return mapAndBalanceProducts(products, activeStoreIds);
//     }

//     @Override
//     public OutfitRecommendation generateOutfit(List<Long> storeIds, String gender, String climate, String style, String material) {
//         logger.info(String.format("Generating outfit with filters: gender=%s, climate=%s, style=%s, material=%s", gender, climate, style, material));
//         List<ProductWithStoreDto> filteredProducts = getProductsWithStores(storeIds, gender, climate, style);

//         if (filteredProducts.isEmpty()) {
//             return new OutfitRecommendation(Collections.emptyList(), "No hay productos disponibles con los filtros aplicados");
//         }

//         if (hasValue(material)) {
//             List<ProductWithStoreDto> materialFiltered = filteredProducts.stream()
//                     .filter(p -> productMatchesMaterial(p.getProduct(), material))
//                     .collect(Collectors.toList());
//             if (materialFiltered.size() >= MIN_PRODUCTS_FOR_OUTFIT) {
//                 filteredProducts = materialFiltered;
//             }
//         }

//         filteredProducts = optimizeProductSelectionForOutfit(filteredProducts);
//         return geminiService.getOutfitRecommendation(gender, climate, style, material, filteredProducts);
//     }

//     @Override
//     public OutfitRecommendation generateOutfitFromChat(String message, String gender) {
//         ChatFilters filters = parseMessageFilters(message);
//         filters.gender = gender;

//         List<ProductWithStoreDto> products = getProductsWithStores(null, filters.gender, filters.climate, filters.style);
//         products = applyAdditionalChatFilters(products, filters);

//         if (products.isEmpty()) {
//             return new OutfitRecommendation(Collections.emptyList(), "No hay productos disponibles con los criterios solicitados");
//         }

//         products = optimizeProductSelectionForOutfit(products);
//         return geminiService.getOutfitRecommendation(filters.gender, filters.climate, filters.style, filters.material, products);
//     }

//     private Set<Long> getActiveAdvertisingStoreIds() {
//         return storeService.getAllStores().stream()
//                 .filter(store -> Boolean.TRUE.equals(store.getActiveAdvertising()))
//                 .map(StoreDto::getStoreId)
//                 .collect(Collectors.toSet());
//     }

//     private List<ProductDto> getFilteredProductsWithFallback(Set<Long> activeStoreIds, String gender, String climate, String style) {
//         List<ProductDto> products;
//         if (hasValue(gender) && hasValue(climate) && hasValue(style)) {
//             products = getRecommendedProducts(new ArrayList<>(activeStoreIds), gender, climate, style);
//             if (hasEnoughProductsForOutfit(products)) return products;
//         }
//         if (hasValue(gender) && hasValue(climate)) {
//             products = getRecommendedProducts(new ArrayList<>(activeStoreIds), gender, climate, null);
//             if (hasEnoughProductsForOutfit(products)) return products;
//         }
//         if (hasValue(gender)) {
//             products = getAllProductsForGender(activeStoreIds, gender, style);
//             if (hasEnoughProductsForOutfit(products)) return products;
//         }
//         return getAllActiveStoreProducts(activeStoreIds, style);
//     }

//     private List<ProductDto> getAllProductsForGender(Set<Long> storeIds, String gender, String style) {
//         List<String> compatibleStyles = new ArrayList<>();
//         if (hasValue(style)) {
//             compatibleStyles.add(style);
//             compatibleStyles.addAll(STYLE_COMPATIBILITY_MAP.getOrDefault(style.toLowerCase(), Collections.emptyList()));
//         }
//         return !compatibleStyles.isEmpty() ? productRepository.findByRecommendedProducts(new ArrayList<>(storeIds), gender, null, compatibleStyles) : productRepository.findByGenderAndStoreIdIn(gender, new ArrayList<>(storeIds));
//     }

//     private List<ProductDto> getAllActiveStoreProducts(Set<Long> storeIds, String style) {
//         List<String> compatibleStyles = new ArrayList<>();
//         if (hasValue(style)) {
//             compatibleStyles.add(style);
//             compatibleStyles.addAll(STYLE_COMPATIBILITY_MAP.getOrDefault(style.toLowerCase(), Collections.emptyList()));
//         }
//         return !compatibleStyles.isEmpty() ? productRepository.findByRecommendedProducts(new ArrayList<>(storeIds), null, null, compatibleStyles) : productRepository.findByStoreIdIn(new ArrayList<>(storeIds));
//     }

//     private List<ProductWithStoreDto> mapAndBalanceProducts(List<ProductDto> products, Set<Long> activeStoreIds) {
//         Map<Long, StoreDto> storeMap = storeService.getAllStores().stream()
//                 .filter(s -> activeStoreIds.contains(s.getStoreId()))
//                 .collect(Collectors.toMap(StoreDto::getStoreId, Function.identity()));

//         List<ProductWithStoreDto> mapped = products.stream()
//                 .filter(p -> p.getStoreId() != null && storeMap.containsKey(p.getStoreId()))
//                 .map(p -> new ProductWithStoreDto(p, storeMap.get(p.getStoreId())))
//                 .collect(Collectors.toList());

//         Set<Long> seenProducts = new HashSet<>();
//         return mapped.stream()
//                 .filter(p -> seenProducts.add(p.getProduct().getIdProduct()))
//                 .collect(Collectors.toList());
//     }

//     private List<ProductWithStoreDto> optimizeProductSelectionForOutfit(List<ProductWithStoreDto> products) {
//         if (products.size() <= MAX_PRODUCTS_FOR_AI) {
//             return products;
//         }

//         // Usa el nuevo `garmentType` para categorizar
//         Map<String, List<ProductWithStoreDto>> categorizedProducts = products.stream()
//             .filter(p -> hasValue(p.getProduct().getGarmentType()))
//             .collect(Collectors.groupingBy(p -> p.getProduct().getGarmentType()));

//         List<ProductWithStoreDto> optimized = new ArrayList<>();
//         for (List<ProductWithStoreDto> categoryList : categorizedProducts.values()) {
//             Collections.shuffle(categoryList);
//             optimized.addAll(categoryList.stream().limit(PRODUCTS_PER_CATEGORY).collect(Collectors.toList()));
//         }

//         if (optimized.size() < MAX_PRODUCTS_FOR_AI) {
//             List<ProductWithStoreDto> remaining = products.stream().filter(p -> !optimized.contains(p)).collect(Collectors.toList());
//             Collections.shuffle(remaining);
//             optimized.addAll(remaining.stream().limit(MAX_PRODUCTS_FOR_AI - optimized.size()).collect(Collectors.toList()));
//         }

//         return optimized.stream().limit(MAX_PRODUCTS_FOR_AI).collect(Collectors.toList());
//     }

//     // ============ LÓGICA DE ENRIQUECIMIENTO DE PRODUCTO ============

//     /**
//      * Centraliza la lógica para añadir atributos de estilismo a un producto.
//      * Se llama antes de guardar un producto.
//      */
//     private void enrichProductDto(ProductDto product) {
//         if (product == null) return;

//         // 1. Determinar GarmentType (el más importante)
//         product.setGarmentType(determineGarmentType(product));

//         // 2. Determinar Familia de Color
//         product.setColorFamily(determineColorFamily(product.getPrimaryColor()));
//     }

//     /**
//      * Lógica mejorada para determinar el tipo de prenda funcional.
//      */
//     private String determineGarmentType(ProductDto product) {
//         String text = (nullSafe(product.getSubcategory()) + " " + nullSafe(product.getName())).toLowerCase();

//         if (containsAny(text, new String[]{"vestido", "enterizo", "jumpsuit", "romper"})) return "DRESS";
//         if (containsAny(text, new String[]{"abrigo", "chaqueta", "saco", "blazer", "chamarra", "cardigan", "gabardina", "parka"})) return "OUTERWEAR";
//         if (containsAny(text, new String[]{"camiseta", "camisa", "blusa", "top", "polo", "sueter", "jersey", "sudadera", "hoodie"})) return "TOP";
//         if (containsAny(text, new String[]{"pantalon", "pantalón", "short", "falda", "jean", "bermuda", "legging", "jogger"})) return "BOTTOM";
//         if (containsAny(text, new String[]{"calzado", "zapato", "bota", "sandalia", "tenis", "mocasin", "zapatilla", "tacon", "tacón"})) return "FOOTWEAR";
//         if (containsAny(text, new String[]{"accesorio", "bolso", "cinturon", "cinturón", "collar", "reloj", "bufanda", "gorra", "sombrero", "gafas", "lentes"})) return "ACCESSORY";
        
//         return "OTHER"; // Categoría por defecto
//     }

//     /**
//      * Determina la familia de color basado en el color primario.
//      */
//     private String determineColorFamily(String color) {
//         if (!hasValue(color)) return "Neutro";
//         String c = color.toLowerCase();

//         if (containsAny(c, new String[]{"rojo", "naranja", "amarillo", "rosado", "fucsia", "terracota"})) return "Cálido";
//         if (containsAny(c, new String[]{"azul", "verde", "violeta", "morado", "turquesa"})) return "Frío";
//         if (containsAny(c, new String[]{"blanco", "negro", "gris", "beige", "marron", "marrón", "crema"})) return "Neutro";

//         return "Neutro"; // Por defecto
//     }

//     private boolean containsAny(String text, String[] keywords) {
//         for (String keyword : keywords) {
//             if (text.contains(keyword)) return true;
//         }
//         return false;
//     }

//     private boolean productMatchesMaterial(ProductDto product, String requestedMaterial) {
//         if (product.getMaterial() == null || requestedMaterial == null) return false;
//         return product.getMaterial().toLowerCase().contains(requestedMaterial.toLowerCase());
//     }

//     private ChatFilters parseMessageFilters(String message) {
//         if (message == null) return new ChatFilters();
//         String msg = message.toLowerCase(Locale.ROOT);
//         ChatFilters filters = new ChatFilters();

//         if (msg.contains("formal") || msg.contains("elegante")) filters.style = "formal";
//         else if (msg.contains("casual") || msg.contains("informal")) filters.style = "casual";
//         else if (msg.contains("deportivo") || msg.contains("sport")) filters.style = "deportivo";

//         if (msg.contains("frío") || msg.contains("frio") || msg.contains("invierno")) filters.climate = "frio";
//         else if (msg.contains("cálido") || msg.contains("calido") || msg.contains("verano") || msg.contains("calor")) filters.climate = "calido";

//         String[] materials = {"lana", "seda", "algodón", "algodon", "cuero", "denim", "jean"};
//         for (String material : materials) {
//             if (msg.contains(material)) {
//                 filters.material = material.replace("algodon", "algodón");
//                 break;
//             }
//         }

//         if (msg.contains("boda") || msg.contains("ceremonia")) filters.style = "formal";
//         else if (msg.contains("oficina") || msg.contains("trabajo")) filters.style = "formal";
//         else if (msg.contains("playa") || msg.contains("piscina")) filters.style = "casual";
//         else if (msg.contains("fiesta") || msg.contains("noche")) filters.style = "elegante";
//         else if (msg.contains("gimnasio") || msg.contains("ejercicio")) filters.style = "deportivo";

//         filters.aspirational = msg.contains("premium") || msg.contains("lujo") || msg.contains("exclusivo");
//         filters.minimalist = msg.contains("minimalista") || msg.contains("básico") || msg.contains("basico");

//         return filters;
//     }

//     private List<ProductWithStoreDto> applyAdditionalChatFilters(List<ProductWithStoreDto> products, ChatFilters filters) {
//         List<ProductWithStoreDto> filtered = new ArrayList<>(products);
//         if (hasValue(filters.material)) {
//             List<ProductWithStoreDto> materialFiltered = filtered.stream().filter(p -> productMatchesMaterial(p.getProduct(), filters.material)).collect(Collectors.toList());
//             if (materialFiltered.size() >= MIN_PRODUCTS_FOR_OUTFIT) filtered = materialFiltered;
//         }
//         if (filters.aspirational && filtered.size() > MIN_PRODUCTS_FOR_OUTFIT * 2) {
//             double avgPrice = filtered.stream().mapToDouble(p -> p.getProduct().getPrice()).average().orElse(0);
//             List<ProductWithStoreDto> premiumFiltered = filtered.stream().filter(p -> p.getProduct().getPrice() >= avgPrice).collect(Collectors.toList());
//             if (premiumFiltered.size() >= MIN_PRODUCTS_FOR_OUTFIT) filtered = premiumFiltered;
//         }
//         if (filters.minimalist && filtered.size() > MIN_PRODUCTS_FOR_OUTFIT * 3) {
//             Collections.shuffle(filtered);
//             filtered = filtered.stream().limit(MIN_PRODUCTS_FOR_OUTFIT * 2).collect(Collectors.toList());
//         }
//         return filtered;
//     }

//     private static boolean hasValue(String value) {
//         return value != null && !value.isBlank();
//     }

//     private static String nullSafe(String value) {
//         return value == null ? "" : value;
//     }

//     private boolean hasEnoughProductsForOutfit(List<ProductDto> products) {
//         return products != null && products.size() >= MIN_PRODUCTS_FOR_OUTFIT;
//     }

//     private static class ChatFilters {
//         String gender, climate, style, material;
//         boolean aspirational = false, minimalist = false;
//     }
// }