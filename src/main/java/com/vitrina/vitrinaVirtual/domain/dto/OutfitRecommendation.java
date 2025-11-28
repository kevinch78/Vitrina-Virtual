package com.vitrina.vitrinaVirtual.domain.dto;

import java.util.List;

public class OutfitRecommendation {
    private List<ProductWithStoreDto> selectedProducts;
    private String accessory;
    private String reason; // Opcional: razón del outfit
    private String imageUrl; // NUEVO campo

    public OutfitRecommendation(List<ProductWithStoreDto> selectedProducts, String accessory) {
        this.selectedProducts = selectedProducts;
        this.accessory = accessory;
    }

    // Getters y Setters
    public List<ProductWithStoreDto> getSelectedProducts() {
        return selectedProducts;
    }

    public void setSelectedProducts(List<ProductWithStoreDto> selectedProducts) {
        this.selectedProducts = selectedProducts;
    }

    public String getAccessory() {
        return accessory;
    }

    public void setAccessory(String accessory) {
        this.accessory = accessory;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}