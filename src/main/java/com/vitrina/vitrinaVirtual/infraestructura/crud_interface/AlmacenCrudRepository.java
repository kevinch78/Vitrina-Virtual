package com.vitrina.vitrinaVirtual.infraestructura.crud_interface;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vitrina.vitrinaVirtual.infraestructura.entity.Almacen;
import java.util.List;


public interface AlmacenCrudRepository extends JpaRepository<Almacen, Long> {
    List<Almacen> findByDireccion(String direccion);
    List<Almacen> findByNombre(String nombre);
    List<Almacen> findByPublicidadActivaTrue();

} 