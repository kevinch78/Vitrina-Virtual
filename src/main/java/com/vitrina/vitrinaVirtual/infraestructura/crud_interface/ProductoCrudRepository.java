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
    List<Producto> findByGeneroAndClimaAndEstilo(String genero, String clima, String estilo);
    List<Producto> findByGenero(String genero);
    List<Producto> findByAlmacenId(Long almacenId);
    // List<Producto> findByCategoria(String categoria);
    List<Producto> findByAlmacenIdInAndGeneroAndClimaAndEstiloIn(List<Long> almacenIds, String genero, String clima, List<String> estilos);
    List<Producto> findByGeneroAndAlmacenIdIn(String genero, List<Long> almacenIds);
    List<Producto> findByAlmacenIdIn(List<Long> almacenIds);
}
