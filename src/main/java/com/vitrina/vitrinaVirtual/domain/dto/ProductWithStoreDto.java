package com.vitrina.vitrinaVirtual.domain.dto;

import lombok.Data;

@Data
public class ProductWithStoreDto {
    private ProductDto product;
    private StoreDto store;

    public ProductWithStoreDto(ProductDto product, StoreDto store) {
    this.product = product;
    this.store = store;
}

}
