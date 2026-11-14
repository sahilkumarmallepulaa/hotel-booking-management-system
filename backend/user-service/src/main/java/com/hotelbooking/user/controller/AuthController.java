package com.hotelbooking.user.controller;

import com.hotelbooking.user.dto.LoginRequestDTO;
import com.hotelbooking.user.dto.LoginResponseDTO;
import com.hotelbooking.user.dto.RegisterRequestDTO;
import com.hotelbooking.user.dto.UserResponseDTO;
import com.hotelbooking.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequestDTO requestDTO) {
        UserResponseDTO userResponse = authService.register(requestDTO);
        return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        LoginResponseDTO loginResponse = authService.login(requestDTO);
        return ResponseEntity.ok(loginResponse);
    }
}
