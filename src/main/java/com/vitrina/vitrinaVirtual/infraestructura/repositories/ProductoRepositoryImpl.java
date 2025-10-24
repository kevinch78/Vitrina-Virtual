package com.vitrina.vitrinaVirtual.infraestructura.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.domain.repository.ProductRepository;
import com.vitrina.vitrinaVirtual.infraestructura.crud_interface.ProductoCrudRepository;
import com.vitrina.vitrinaVirtual.infraestructura.entity.EntityPreprocessor;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Producto;
import com.vitrina.vitrinaVirtual.infraestructura.mapper.ProductoMapper;

@Repository
public class ProductoRepositoryImpl implements ProductRepository {
    @Autowired 
    private ProductoCrudRepository productoCrudRepository;
    @Autowired
    private ProductoMapper productoMapper;
    @Autowired
    private EntityPreprocessor entityPreprocessor;

    @Override
    public List<ProductDto> findAll() {
        List<Producto> productos = (List<Producto>)productoCrudRepository.findAll();
        return productoMapper.toProductDtos(productos);
    }

    @Override
    public ProductDto findById(Long productId) {
        Optional<Producto> producto = productoCrudRepository.findById(productId);
        return producto.map(productoMapper::toProductDto)
                .orElseThrow(() -> new RuntimeException("Producto con ID " + productId + " no encontrado"));
    }

    @Override
    public ProductDto save(ProductDto productDto) {
        Producto producto = entityPreprocessor.preprocessProduct(productDto);
        Producto savedProducto = productoCrudRepository.save(producto);
        return productoMapper.toProductDto(savedProducto);
    }

    @Override
    public void deleteById(Long productId) {
        productoCrudRepository.deleteById(productId);
    }

    @Override
    public List<ProductDto> findByStyle(String style) {
        List<Producto> productos = productoCrudRepository.findByEstilo(style);
        return productoMapper.toProductDtos(productos);
    }

    @Override
    public List<ProductDto> findByStoreId(Long storeId) {
        List<Producto> productos = productoCrudRepository.findByAlmacenId(storeId);
        return productoMapper.toProductDtos(productos);
    }

    // @Override
    // public List<ProductDto> findByCategory(String category) {
    //     List<Producto> productos = productoCrudRepository.findByCategoria(category);
    //     return productoMapper.toProductDtos(productos);
    // }

    @Override
    public List<ProductDto> findByGender(String gender) {
        List<Producto> productos = productoCrudRepository.findByGenero(gender);
        return productoMapper.toProductDtos(productos);
    }

    @Override
    public List<ProductDto> findByRecommendedProducts(List<Long> storeIds, String gender, String climate,
            String style) {
        // CORRECCIÓN: La consulta ahora maneja filtros nulos correctamente.
        // Si un filtro es nulo, no se aplica.
        if (style == null && climate == null && gender == null) {
            return findByStoreIdIn(storeIds);
        }
        if (style == null && climate == null) {
            return findByGenderAndStoreIdIn(gender, storeIds);
        }

        // Esta es una simplificación. Para una solución completa, se necesitaría
        // una consulta dinámica con Criteria API o JPQL. Por ahora, esto mejora el comportamiento.
        List<Producto> productos = productoCrudRepository.findByAlmacenIdIn(storeIds).stream()
            .filter(p -> gender == null || gender.equalsIgnoreCase(p.getGenero()))
            .filter(p -> climate == null || climate.equalsIgnoreCase(p.getClima()))
            .filter(p -> style == null || style.equalsIgnoreCase(p.getEstilo()))
            .collect(java.util.stream.Collectors.toList());

        return productoMapper.toProductDtos(productos);
    }

    @Override
    public List<ProductDto> findByRecommendedProducts(List<Long> storeIds, String gender, String climate,
            List<String> styles) {
        List<Producto> productos;
        
        if (storeIds == null || storeIds.isEmpty()) {
            productos = List.of(); 
        } else {
            productos = productoCrudRepository.findByAlmacenIdInAndGeneroAndClimaAndEstiloIn(storeIds, gender, climate, styles);
        }
        
        return productoMapper.toProductDtos(productos);
    }

    @Override
    public List<ProductDto> findByGenderAndStoreIdIn(String gender, List<Long> storeIds) {
        return productoMapper.toProductDtos(productoCrudRepository.findByGeneroAndAlmacenIdIn(gender, storeIds));
    }

    @Override
    public List<ProductDto> findByStoreIdIn(List<Long> storeIds) {
        return productoMapper.toProductDtos(productoCrudRepository.findByAlmacenIdIn(storeIds));
    }
}
