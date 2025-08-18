package com.vitrina.vitrinaVirtual.domain.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;
import com.vitrina.vitrinaVirtual.domain.repository.ProductRepository;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StoreService storeService;

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

    public List<ProductWithStoreDto> getProductsWithStores(List<Long> storeIds, String gender, String climate, String style) {
    // Obtener productos recomendados
    List<ProductDto> products = getRecommendedProducts(storeIds, gender, climate, style);

    // Obtener todos los almacenes
    List<StoreDto> stores = storeService.getAllStores();

    // Crear un mapa de almacenes por su ID para acceso rápido
    Map<Long, StoreDto> storeMap = stores.stream()
            .collect(Collectors.toMap(StoreDto::getStoreId, store -> store));

    // Combinar cada producto con su almacén correspondiente
    return products.stream()
            .filter(product -> product.getStoreId() != null)
            .map(product -> {
                StoreDto store = storeMap.get(product.getStoreId());
                return new ProductWithStoreDto(product, store != null ? store : new StoreDto());
            })
            .collect(Collectors.toList());
}
    
}
