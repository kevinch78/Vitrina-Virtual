package com.vitrina.vitrinaVirtual.infraestructura.crud_interface;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;


public interface UsuarioCrudRepository extends JpaRepository<Usuario, Long> {
    Usuario findByNombre(String nombre);
}
