package com.hotelbooking.user.service;

import com.hotelbooking.user.dto.LoginRequestDTO;
import com.hotelbooking.user.dto.LoginResponseDTO;
import com.hotelbooking.user.dto.RegisterRequestDTO;
import com.hotelbooking.user.dto.UserResponseDTO;

public interface AuthService {
    UserResponseDTO register(RegisterRequestDTO requestDTO);
    LoginResponseDTO login(LoginRequestDTO requestDTO);
}
