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

    // @Override
    // public ProductDto save(ProductDto productDto) {
    //     Producto producto = productoMapper.toProducto(productDto);
    //     Producto productoSaved = productoCrudRepository.save(producto);
    //     return productoMapper.toProductDto(productoSaved);
    // }
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

    @Override
    public List<ProductDto> findByCategory(String category) {
        List<Producto> productos = productoCrudRepository.findByCategoria(category);
        return productoMapper.toProductDtos(productos);
    }

    @Override
    public List<ProductDto> findByRecommendedProducts(List<Long> storeIds, String gender, String climate,
            String style) {
                List<Producto> productos = productoCrudRepository.findByAlmacenIdInAndGeneroAndClimaAndEstilo(storeIds, gender, climate, style);
                return productoMapper.toProductDtos(productos);
    }
    
}
