package com.vitrina.vitrinaVirtual.controller;

import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import com.vitrina.vitrinaVirtual.domain.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cloudinary.Cloudinary;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private Cloudinary cloudinary;
    private static final Log logger = LogFactory.getLog(ProductController.class);
    private final ObjectMapper objectMapper = new ObjectMapper(); // Para deserializar JSON

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductDto> createProduct(
            @RequestPart("productDto") String productDtoJson, // Recibe el JSON como string
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Executing createProduct for user: " + username); // Usa el logger
        // Deserializa el JSON a ProductDto
        ProductDto productDto = objectMapper.readValue(productDtoJson, ProductDto.class);
        if (image != null && !image.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader()
                    .upload(image.getBytes(), Map.of(
                            "folder", "vitrina_virtual/products",
                            "public_id", productDto.getName() + "_" + System.currentTimeMillis(),
                            "resource_type", "image"
                    ));
            productDto.setImageUrl(uploadResult.get("secure_url").toString());
        }
        return new ResponseEntity<>(productService.saveProduct(productDto), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        logger.debug("Fetching all products");
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long productId) {
        logger.debug("Fetching product with id: " + productId);
        ProductDto product = productService.getProductById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Producto no encontrado");
        }
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteProductById(@PathVariable Long ProductId) {
        logger.debug("Deleting product with id: " + ProductId);
        productService.deleteProductById(ProductId);
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
            @RequestParam String style) {
        logger.debug("Fetching recommended products with filters: storeIds=" + storeIds + ", gender=" + gender + ", climate=" + climate + ", style=" + style);       
        return ResponseEntity.ok(productService.getRecommendedProducts(storeIds, gender, climate, style));
    }
    @GetMapping("/with-stores")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CLIENT', 'ROLE_SELLER')")
    public ResponseEntity<List<ProductWithStoreDto>> getProductsWithStores(
            @RequestParam(required = false) List<Long> storeIds,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String climate,
            @RequestParam(required = false) String style) {
        logger.debug("Fetching recommended products with filters: storeIds=" + storeIds + ", gender=" + gender + ", climate=" + climate + ", style=" + style);       
        return ResponseEntity.ok(productService.getProductsWithStores(storeIds, gender, climate, style));
    }
}