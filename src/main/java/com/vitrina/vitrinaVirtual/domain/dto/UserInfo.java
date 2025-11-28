package com.vitrina.vitrinaVirtual.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Solo incluye campos que no sean null
public class UserInfo {
    private Long id;
    private String email;
    private String role;
    
    // Para VENDOR
    private Long storeId;
    
    // Para CLIENTE
    private Long clientId;
}