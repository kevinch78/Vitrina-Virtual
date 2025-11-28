package com.vitrina.vitrinaVirtual.domain.service;

import com.vitrina.vitrinaVirtual.domain.dto.ClienteDto;
import com.vitrina.vitrinaVirtual.domain.repository.ClienteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteServiceImpl implements ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public ClienteDto createCliente(ClienteDto clienteDto) {

        if (clienteDto.getUsuarioId() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario dueño del perfil");
        }

        if (clienteRepository.existsByUsuarioId(clienteDto.getUsuarioId())) {
            throw new IllegalStateException("Este usuario ya tiene un perfil de cliente");
        }

        return clienteRepository.save(clienteDto);
    }

    @Override
    public Optional<ClienteDto> getClienteById(Long id) {
        return clienteRepository.findById(id);
    }

    @Override
    public List<ClienteDto> getAllClientes() {
        return clienteRepository.findAll();
    }
}
