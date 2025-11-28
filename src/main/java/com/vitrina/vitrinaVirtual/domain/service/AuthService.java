package com.vitrina.vitrinaVirtual.domain.service;

import com.vitrina.vitrinaVirtual.domain.dto.AuthResponse;
import com.vitrina.vitrinaVirtual.domain.dto.LoginRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.RegistrationRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserProfileDto;

public interface AuthService {
    UserDto register(RegistrationRequestDto registrationRequestDto);
    AuthResponse login(LoginRequestDto loginRequestDto);
    UserProfileDto getUserProfile(String username);

    
} 
