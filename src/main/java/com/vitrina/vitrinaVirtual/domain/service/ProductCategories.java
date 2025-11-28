package com.vitrina.vitrinaVirtual.domain.service;

import com.vitrina.vitrinaVirtual.domain.dto.ProductWithStoreDto;
import java.util.ArrayList;
import java.util.List;

public class ProductCategories {
    private List<ProductWithStoreDto> tops = new ArrayList<>();
    private List<ProductWithStoreDto> bottoms = new ArrayList<>();
    private List<ProductWithStoreDto> footwear = new ArrayList<>();
    private List<ProductWithStoreDto> outerwear = new ArrayList<>();
    private List<ProductWithStoreDto> accessories = new ArrayList<>();
    private List<ProductWithStoreDto> others = new ArrayList<>();

    // Getters
    public List<ProductWithStoreDto> getTops() {
        return tops;
    }

    public List<ProductWithStoreDto> getBottoms() {
        return bottoms;
    }

    public List<ProductWithStoreDto> getFootwear() {
        return footwear;
    }

    public List<ProductWithStoreDto> getOuterwear() {
        return outerwear;
    }

    public List<ProductWithStoreDto> getAccessories() {
        return accessories;
    }

    public List<ProductWithStoreDto> getOthers() {
        return others;
    }
}
