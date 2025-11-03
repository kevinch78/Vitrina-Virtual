package com.vitrina.vitrinaVirtual.controller;

import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import com.vitrina.vitrinaVirtual.domain.dto.OutfitRecommendation;
import com.vitrina.vitrinaVirtual.domain.service.ProductService;
import com.vitrina.vitrinaVirtual.domain.service.CloudinaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cloudinary.Cloudinary;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.*;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Productos", description = "Gestión de productos y generación de outfits con IA")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {
    @Autowired private ProductService productService;
    @Autowired private CloudinaryService cloudinaryService; // Inyectamos el nuevo servicio
    private static final Log logger = LogFactory.getLog(ProductController.class);

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear producto", description = "Crea un nuevo producto con imagen opcional")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos del producto inválidos"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo administradores")
    })
    public ResponseEntity<ProductDto> createProduct(
            @Parameter(description = "Datos del producto en formato JSON") @RequestPart("productDto") ProductDto productDto,
            @Parameter(description = "Imagen del producto") @RequestPart(value = "image", required = false) MultipartFile image
    ) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Executing createProduct for user: " + username);

        String imageBase64 = null;
        if (image != null && !image.isEmpty()) {
            CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadImageAndGetBase64(image, "vitrina_virtual/products", productDto.getName());
            productDto.setImageUrl(uploadResult.url());
            imageBase64 = uploadResult.base64();
        }
        return new ResponseEntity<>(productService.saveProduct(productDto, imageBase64), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        logger.debug("Fetching all products");
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductDto> getProductById(@PathVariable("productId") Long productId) {
        logger.debug("Fetching product with id: " + productId);
        ProductDto product = productService.getProductById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Producto no encontrado");
        }
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteProductById(@PathVariable("productId") Long productId) {
        logger.debug("Deleting product with id: " + productId);
        productService.deleteProductById(productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/style/{style}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLIENT', 'ROLE_SELLER')")
    public ResponseEntity<List<ProductDto>> getProductsByStyle(@PathVariable String style) {
        logger.debug("Fetching products by style: " + style);
        return ResponseEntity.ok(productService.getProductsByStyle(style));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLIENT', 'ROLE_SELLER')")
    public ResponseEntity<List<ProductDto>> getProductsByStoreId(@PathVariable Long storeId) {
        logger.debug("Fetching products by storeId: " + storeId);
        return ResponseEntity.ok(productService.getProductsByStoreId(storeId));
    }

    @GetMapping("/recommended")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLIENT', 'ROLE_SELLER')")
    public ResponseEntity<List<ProductDto>> getRecommendedProducts(
            @RequestParam List<Long> storeIds,
            @RequestParam String gender,
            @RequestParam String climate,
            @RequestParam String style
    ) {
        logger.debug("Fetching recommended products with filters: storeIds=" + storeIds + ", gender=" + gender + ", climate=" + climate + ", style=" + style);
        return ResponseEntity.ok(productService.getRecommendedProducts(storeIds, gender, climate, style));
    }

    @GetMapping("/with-stores")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLIENT', 'ROLE_SELLER')")
    public ResponseEntity<List<ProductWithStoreDto>> getProductsWithStores(
            @RequestParam(required = false) List<Long> storeIds,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String climate,
            @RequestParam(required = false) String style
    ) {
        logger.debug("Fetching products WITH stores: storeIds=" + storeIds + ", gender=" + gender + ", climate=" + climate + ", style=" + style);
        return ResponseEntity.ok(productService.getProductsWithStores(storeIds, gender, climate, style));
    }

    @GetMapping("/outfit")
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    @Operation(summary = "Generar outfit con IA", description = "Genera recomendaciones de outfit usando inteligencia artificial")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Outfit generado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo clientes")
    })
    public ResponseEntity<OutfitRecommendation> generateOutfit(
            @Parameter(description = "IDs de las tiendas a considerar") @RequestParam(required = false) List<Long> storeIds,
            @Parameter(description = "Género (masculino/femenino)") @RequestParam String gender,
            @Parameter(description = "Clima (caliente/templado/frío)") @RequestParam String climate,
            @Parameter(description = "Estilo de vestimenta") @RequestParam String style,
            @Parameter(description = "Material preferido") @RequestParam(required = false) String material
    ) {
        OutfitRecommendation outfit = productService.generateOutfit(storeIds, gender, climate, style, material);
        return ResponseEntity.ok(outfit);
    }

    // Chat → si lo usarás con clientes, puedes cambiar a ROLE_CLIENT o añadir otro endpoint aparte
    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OutfitRecommendation> generateOutfitFromChat(
            @RequestParam String gender,
            @RequestBody Map<String, String> request
    ) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new OutfitRecommendation(Collections.emptyList(), "Error: Mensaje vacío"));
        }
        OutfitRecommendation recommendation = productService.generateOutfitFromChat(message, gender);
        return ResponseEntity.ok(recommendation);
    }

    // Endpoint temporal para debugging
    @GetMapping("/debug/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> debugAllProducts() {
        List<ProductDto> allProducts = productService.getAllProducts();
        Map<String, Object> response = new HashMap<>();
        response.put("totalProducts", allProducts.size());
        response.put("products", allProducts);
        return ResponseEntity.ok(response);
    }

}
