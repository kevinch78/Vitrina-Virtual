package com.vitrina.vitrinaVirtual.infraestructura.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.vitrina.vitrinaVirtual.domain.dto.ClienteDto;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Cliente;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Usuario;


@Mapper(componentModel = "spring")
public interface ClienteMapper {

    @Mapping(target = "usuarioId", source = "usuario.idUsuario")
    ClienteDto toClienteDto(Cliente cliente);

    @Mapping(target = "usuario", source = "usuarioId", qualifiedByName = "usuarioFromId")
    Cliente toCliente(ClienteDto clienteDto);

    @Named("usuarioFromId")
    public static Usuario usuarioFromId(Long id) {
        if (id == null) return null;
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        return u;
    }
}



