package com.vitrina.vitrinaVirtual.domain.dto;

import lombok.Data;

@Data
public class ProductDto {
    private Long idProduct;
    private String name;
    private Double price;
    private int stock; 
    private String description;
    private String style;
    private String climate;
    private String gender;
    private String category;
    private String color;
    private String material;
    private String occasion;
    private String imageUrl; 
    private Long storeId;

}
