package com.vitrina.vitrinaVirtual.domain.repository;
import java.util.List;
import com.vitrina.vitrinaVirtual.domain.dto.ClienteDto;
import java.util.Optional;

public interface ClienteRepository {

    ClienteDto save(ClienteDto clienteDto);

    Optional<ClienteDto> findById(Long id);

    List<ClienteDto> findAll();

    boolean existsByUsuarioId(Long id);
}

