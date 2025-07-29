package com.vitrina.vitrinaVirtual.domain.repository;

import java.util.List;

import com.vitrina.vitrinaVirtual.domain.dto.LoginRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.RegistrationRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserDto;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;

public interface UserRepository {
    UserDto save(UserDto userDto);
    UserDto save(RegistrationRequestDto registrationRequestDto);
    UserDto findByUserName(String name);
    LoginRequestDto findLoginRequestByUsername(String name);
    List<UserDto> findAll();
    Usuario findByUsernameFromEntity(String name); // Nuevo método para acceder a la entidad
}
