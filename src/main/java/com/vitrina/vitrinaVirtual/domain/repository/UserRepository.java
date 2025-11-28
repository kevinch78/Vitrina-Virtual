package com.vitrina.vitrinaVirtual.domain.repository;

import java.util.List;
import java.util.Optional;
import com.vitrina.vitrinaVirtual.domain.dto.RegistrationRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserDto;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;

public interface UserRepository {
    UserDto save(UserDto userDto);
    UserDto save(RegistrationRequestDto registrationRequestDto);
    UserDto findByUserName(String name);
    List<UserDto> findAll();
    Usuario findByUsernameFromEntity(String name); // Nuevo método para acceder a la entidad
    Optional<Usuario> findByEmailFromEntity(String email);
    Usuario saveEntity(Usuario usuario); // NUEVO: para guardar directamente la entidad

}
