package com.vitrina.vitrinaVirtual.domain.service;

import com.vitrina.vitrinaVirtual.domain.dto.ClienteDto;

import java.util.List;
import java.util.Optional;

public interface ClienteService {

    ClienteDto createCliente(ClienteDto clienteDto);

    Optional<ClienteDto> getClienteById(Long id);

    List<ClienteDto> getAllClientes();
}
