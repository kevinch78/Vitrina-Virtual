package com.vitrina.vitrinaVirtual.infraestructura.crud_interface;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;


public interface UsuarioCrudRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByNombre(String nombre);
    Optional<Usuario> findByCorreo(String correo);
}
