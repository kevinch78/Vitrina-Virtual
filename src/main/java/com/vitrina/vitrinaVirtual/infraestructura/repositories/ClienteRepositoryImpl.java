package com.vitrina.vitrinaVirtual.infraestructura.repositories;

import com.vitrina.vitrinaVirtual.domain.dto.ClienteDto;
import com.vitrina.vitrinaVirtual.domain.repository.ClienteRepository;
import com.vitrina.vitrinaVirtual.infraestructura.crud_interface.ClienteCrudRepository;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Cliente;
import com.vitrina.vitrinaVirtual.infraestructura.mapper.ClienteMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepositoryImpl implements ClienteRepository {

    @Autowired
    private ClienteCrudRepository clienteCrudRepository;

    @Autowired
    private ClienteMapper clienteMapper;

    @Override
    public ClienteDto save(ClienteDto clienteDto) {
        Cliente cliente = clienteMapper.toCliente(clienteDto);
        Cliente clienteSaved = clienteCrudRepository.save(cliente);
        return clienteMapper.toClienteDto(clienteSaved);
    }

    @Override
    public Optional<ClienteDto> findById(Long id) {
        return clienteCrudRepository.findById(id)
                .map(clienteMapper::toClienteDto);
    }

    @Override
    public List<ClienteDto> findAll() {
        return ((List<Cliente>) clienteCrudRepository.findAll())
                .stream()
                .map(clienteMapper::toClienteDto)
                .toList();
    }

    @Override
    public boolean existsByUsuarioId(Long idUsuario) {
        return clienteCrudRepository.existsByUsuarioIdUsuario(idUsuario);
    }
}
