package com.vitrina.vitrinaVirtual.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetDto {
    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres.")
    private String newPassword;
}