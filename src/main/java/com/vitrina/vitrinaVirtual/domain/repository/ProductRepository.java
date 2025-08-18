package com.vitrina.vitrinaVirtual.domain.repository;

import java.util.List;
import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;

public interface ProductRepository {
    List<ProductDto> findAll();
    ProductDto findById(Long productId);
    ProductDto save(ProductDto productDto);
    void deleteById(Long productId);
    List<ProductDto> findByStyle(String style);
    List<ProductDto> findByStoreId(Long storeId);
    List<ProductDto> findByCategory(String category);
    List<ProductDto> findByRecommendedProducts(List<Long> storeIds, String gender, String climate , String style);

}
