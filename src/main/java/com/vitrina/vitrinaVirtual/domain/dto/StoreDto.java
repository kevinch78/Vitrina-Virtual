package com.vitrina.vitrinaVirtual.domain.dto;

import lombok.Data;

@Data
public class StoreDto {
    private Long storeId;
    private String name;
    private String description;
    private String city;
    private String address;
    private String contact;
    private String owner;
    private String imageUrl;
    private Boolean activeAdvertising;
  
}
