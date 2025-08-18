package com.vitrina.vitrinaVirtual.domain.service;

import java.util.List;

import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;

public interface ProductService {
    List<ProductDto> getAllProducts();
    ProductDto getProductById(Long productId);
    ProductDto saveProduct(ProductDto productDto);
    void deleteProductById(Long productId);
    List<ProductDto> getProductsByStyle(String style);
    List<ProductDto> getProductsByStoreId(Long storeId);
    List<ProductDto> getRecommendedProducts(List<Long> storeIds, String gender, String climate, String style);
    List<ProductWithStoreDto> getProductsWithStores(List<Long> storeIds, String gender, String climate, String style);
}
