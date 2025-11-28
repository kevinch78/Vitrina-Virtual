package com.vitrina.vitrinaVirtual.domain.service;

import java.util.List;

import com.vitrina.vitrinaVirtual.domain.dto.OutfitRecommendation; // Corregido para que apunte a la clase correcta
import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;

public interface ProductService {
    List<ProductDto> getAllProducts();
    ProductDto getProductById(Long productId);
    ProductDto saveProduct(ProductDto productDto, String imageBase64);
    void deleteProductById(Long productId);
    List<ProductDto> getProductsByStyle(String style);
    List<ProductDto> getProductsByStoreId(Long storeId);
    List<ProductDto> getRecommendedProducts(List<Long> storeIds, String gender, String climate, String style);
    List<ProductWithStoreDto> getProductsWithStores(List<Long> storeIds, String gender, String climate, String style);
    OutfitRecommendation generateOutfit(List<Long> storeIds, String gender, String climate, String style,
            String material, boolean generateImage);
    OutfitRecommendation generateOutfitFromChat(String message, String gender, boolean generateImage);
}
