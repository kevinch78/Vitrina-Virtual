package com.vitrina.vitrinaVirtual.infraestructura.crud_interface;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vitrina.vitrinaVirtual.infraestructura.entity.Producto;
import java.util.List;
import java.util.Optional;


public interface ProductoCrudRepository extends JpaRepository <Producto, Long> {
    List<Producto> findByEstilo(String estilo);
    Optional<Producto> findByIdProducto(Long idProducto);
    void deleteById(Long almacenId);
    List<Producto> findByAlmacenIdInAndGeneroAndClimaAndEstilo(List<Long> almacenIds, String genero, String clima, String estilo);
    List<Producto> findByAlmacenId(Long almacenId);
    List<Producto> findByCategoria(String categoria);
    
}
