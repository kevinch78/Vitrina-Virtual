package com.vitrina.vitrinaVirtual.infraestructura.mapper;

import java.util.List;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vitrina.vitrinaVirtual.domain.dto.StoreDto;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Almacen;

@Mapper(componentModel = "spring")
public interface AlmacenMapper {
    @Mapping(source = "id", target = "storeId")
    @Mapping(source = "nombre", target = "name")
    @Mapping(source = "descripcion", target = "description")
    @Mapping(source = "ciudad", target = "city")
    @Mapping(source = "direccion", target = "address")
    @Mapping(source = "contacto", target = "contact")
    @Mapping(source = "propietario.nombre", target = "owner")
    @Mapping(source = "imagenUrl", target = "imageUrl")
    @Mapping(source = "publicidadActiva", target = "activeAdvertising")

    StoreDto toStoreDto(Almacen almacen);
    List<StoreDto> toStoreDtos(List<Almacen> almacenes);

    @InheritInverseConfiguration
    @Mapping(target = "propietario", ignore = true)
    @Mapping(target = "productos", ignore = true)
    Almacen toAlmacen(StoreDto storeDto);
    List<Almacen> toAlmacenes(List<StoreDto> storeDtos);


}
