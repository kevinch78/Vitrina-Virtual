package com.vitrina.vitrinaVirtual.infraestructura.mapper;
import java.util.List;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vitrina.vitrinaVirtual.domain.dto.ProductDto;
import com.vitrina.vitrinaVirtual.infraestructura.entity.Producto;

@Mapper(componentModel = "spring")
public interface ProductoMapper {
    @Mapping(source = "idProducto", target = "idProduct")
    @Mapping(source = "nombre", target = "name")
    @Mapping(source = "precio", target = "price")
    @Mapping(source = "existencia", target = "stock")
    @Mapping(source = "descripcion", target = "description")
    @Mapping(source = "estilo", target = "style")
    @Mapping(source = "clima", target = "climate")
    @Mapping(source = "genero", target = "gender")
    @Mapping(source = "categoria", target = "category")
    @Mapping(source = "color", target = "color")
    @Mapping(source = "material", target = "material")
    @Mapping(source = "ocasion", target = "occasion")
    @Mapping(source = "imagenUrl", target = "imageUrl")
    @Mapping(source = "almacen.id", target = "storeId")

    ProductDto toProductDto(Producto producto);
    List<ProductDto> toProductDtos(List<Producto> productos);

    @InheritInverseConfiguration
    Producto toProducto(ProductDto productDto);
    List<Producto> toProductos(List<ProductDto> productDtos);

}
