package com.vitrina.vitrinaVirtual.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vitrina.vitrinaVirtual.domain.dto.LoginRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.RegistrationRequestDto;
import com.vitrina.vitrinaVirtual.domain.dto.UserDto;
import com.vitrina.vitrinaVirtual.domain.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity <UserDto> register(@RequestBody RegistrationRequestDto registrationRequestDto) {
        return new ResponseEntity<>(authService.register(registrationRequestDto),HttpStatus.CREATED);
    }
    @PostMapping("/login")
    public ResponseEntity <String> login(@RequestBody LoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }
    
    
    
}
