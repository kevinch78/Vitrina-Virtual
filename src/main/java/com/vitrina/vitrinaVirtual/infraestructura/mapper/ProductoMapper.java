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
    @Mapping(source = "subcategoria", target = "subcategory")
    @Mapping(source = "primaryColor", target = "primaryColor")
    @Mapping(source = "secondaryColors", target = "secondaryColors")
    @Mapping(source = "colorFamily", target = "colorFamily")
    @Mapping(source = "pattern", target = "pattern")
    @Mapping(source = "fit", target = "fit")
    @Mapping(source = "formality", target = "formality")
    @Mapping(source = "garmentType", target = "garmentType")
    @Mapping(source = "material", target = "material")
    @Mapping(source = "ocasion", target = "occasion")
    @Mapping(source = "imagenUrl", target = "imageUrl")
    @Mapping(source = "iaDescription", target = "iaDescription")
    @Mapping(source = "technicalDescription", target = "technicalDescription")
    @Mapping(source = "almacen.id", target = "storeId")

    ProductDto toProductDto(Producto producto);
    List<ProductDto> toProductDtos(List<Producto> productos);

    @InheritInverseConfiguration // Mantenemos esto para los campos que coinciden
    @Mapping(source = "idProduct", target = "idProducto")
    @Mapping(source = "name", target = "nombre")
    @Mapping(source = "price", target = "precio")
    @Mapping(source = "stock", target = "existencia")
    @Mapping(source = "description", target = "descripcion")
    @Mapping(source = "style", target = "estilo")
    @Mapping(source = "climate", target = "clima")
    @Mapping(source = "gender", target = "genero")
    @Mapping(source = "subcategory", target = "subcategoria")
    @Mapping(source = "primaryColor", target = "primaryColor")
    @Mapping(source = "secondaryColors", target = "secondaryColors")
    @Mapping(source = "pattern", target = "pattern")
    @Mapping(source = "fit", target = "fit")
    @Mapping(source = "formality", target = "formality")
    @Mapping(source = "garmentType", target = "garmentType") // Mapeo inverso explícito
    @Mapping(source = "material", target = "material")
    @Mapping(source = "occasion", target = "ocasion")
    @Mapping(source = "imageUrl", target = "imagenUrl")
    @Mapping(source = "iaDescription", target = "iaDescription")
    @Mapping(source = "technicalDescription", target = "technicalDescription")
    @Mapping(target = "almacen", ignore = true) // Ignoramos el objeto completo para evitar ciclos
    Producto toProducto(ProductDto productDto);
    List<Producto> toProductos(List<ProductDto> productDtos);

}
