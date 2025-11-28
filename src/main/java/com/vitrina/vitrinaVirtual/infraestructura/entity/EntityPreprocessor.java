package com.vitrina.vitrinaVirtual.infraestructura.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vitrina.vitrinaVirtual.infraestructura.crud_interface.AlmacenCrudRepository;
import com.vitrina.vitrinaVirtual.infraestructura.crud_interface.UsuarioCrudRepository; 
import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;

@Component
public class EntityPreprocessor {
    @Autowired
    private AlmacenCrudRepository almacenCrudRepository;

    @Autowired
    private UsuarioCrudRepository usuarioCrudRepository; // Cambiado a UsuarioCrudRepository

    public Producto preprocessProduct(ProductDto productDto) {
        Producto producto = new Producto();
        producto.setNombre(productDto.getName());
        producto.setPrecio(productDto.getPrice());
        producto.setExistencia(productDto.getStock());
        producto.setDescripcion(productDto.getDescription());
        producto.setEstilo(productDto.getStyle());
        producto.setClima(productDto.getClimate());
        producto.setGenero(productDto.getGender());
        producto.setOcasion(productDto.getOccasion());
        producto.setMaterial(productDto.getMaterial());
        producto.setGarmentType(productDto.getGarmentType());
        producto.setSubcategoria(productDto.getSubcategory());
        producto.setPrimaryColor(productDto.getPrimaryColor());
        producto.setSecondaryColors(productDto.getSecondaryColors());
        producto.setColorFamily(productDto.getColorFamily());
        producto.setPattern(productDto.getPattern());
        producto.setFit(productDto.getFit());
        producto.setFormality(productDto.getFormality());
        producto.setImagenUrl(productDto.getImageUrl());
        producto.setIaDescription(productDto.getIaDescription()); // <-- ¡LA LÍNEA QUE FALTABA!
        producto.setTechnicalDescription(productDto.getTechnicalDescription());
        
        if (productDto.getStoreId() != null) {
            Almacen almacen = almacenCrudRepository.findById(productDto.getStoreId())
                .orElseThrow(() -> new RuntimeException("Almacen con ID " + productDto.getStoreId() + " no encontrado"));
            producto.setAlmacen(almacen);
        }
        return producto;
    }

    public Almacen preprocessStore(StoreDto storeDto) {
        Almacen almacen = new Almacen();
        almacen.setNombre(storeDto.getName());
        almacen.setDescripcion(storeDto.getDescription());
        almacen.setCiudad(storeDto.getCity());
        almacen.setDireccion(storeDto.getAddress());
        almacen.setContacto(storeDto.getContact());
        almacen.setImagenUrl(storeDto.getImageUrl());
        almacen.setPublicidadActiva(storeDto.getActiveAdvertising()); // Usamos el campo correcto
        if (storeDto.getOwner() != null) {
            Usuario usuario = usuarioCrudRepository.findByNombre(storeDto.getOwner()) // Cambiado a findByNombre
                .orElseThrow(() -> new RuntimeException("Usuario con nombre " + storeDto.getOwner() + " no encontrado"));
            almacen.setPropietario(usuario); // Asignamos la entidad Usuario
        }
        return almacen;
    }
}
