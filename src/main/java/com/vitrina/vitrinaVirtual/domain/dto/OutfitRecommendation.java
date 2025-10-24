package com.vitrina.vitrinaVirtual.domain.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OutfitRecommendation {
    private final List<ProductWithStoreDto> prendas;
    private final String accesorio;

    public OutfitRecommendation(List<ProductWithStoreDto> prendas, String accesorio) {
        this.prendas = prendas == null ? Collections.emptyList() : new ArrayList<>(prendas);
        this.accesorio = accesorio == null ? "" : accesorio;
    }

    public List<ProductWithStoreDto> getPrendas() {
        return new ArrayList<>(prendas);
    }

    public String getAccesorio() {
        return accesorio;
    }
    
    @Override
    public String toString() {
        return String.format("OutfitRecommendation{prendas=%d items, accesorio='%s'}", 
            prendas.size(), accesorio);
    }
}
