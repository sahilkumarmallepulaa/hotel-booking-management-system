package com.hotelbooking.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotelbooking.user.controller.AuthController;
import com.hotelbooking.user.dto.LoginRequestDTO;
import com.hotelbooking.user.dto.LoginResponseDTO;
import com.hotelbooking.user.dto.RegisterRequestDTO;
import com.hotelbooking.user.dto.UserResponseDTO;
import com.hotelbooking.user.entity.Role;
import com.hotelbooking.user.exception.GlobalExceptionHandler;
import com.hotelbooking.user.exception.InvalidCredentialsException;
import com.hotelbooking.user.exception.UserAlreadyExistsException;
import com.hotelbooking.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testRegister_Success() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("Test User", "test@example.com", "Password@123");
        UserResponseDTO response = new UserResponseDTO(1L, "Test User", "test@example.com", Role.USER, LocalDateTime.now());

        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(authService, times(1)).register(any(RegisterRequestDTO.class));
    }

    @Test
    void testRegister_InvalidEmail_Returns400() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("Test User", "not-a-valid-email", "Password@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());

        verify(authService, never()).register(any(RegisterRequestDTO.class));
    }

    @Test
    void testRegister_MissingFields_Returns400() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("", "", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        verify(authService, never()).register(any(RegisterRequestDTO.class));
    }

    @Test
    void testRegister_ShortPassword_Returns400() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("Test User", "test@example.com", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        verify(authService, never()).register(any(RegisterRequestDTO.class));
    }

    @Test
    void testRegister_DuplicateEmail_Returns409() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("Test User", "existing@example.com", "Password@123");

        when(authService.register(any(RegisterRequestDTO.class)))
                .thenThrow(new UserAlreadyExistsException("Email is already registered: existing@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered: existing@example.com"));

        verify(authService, times(1)).register(any(RegisterRequestDTO.class));
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("test@example.com", "Password@123");
        UserResponseDTO userResponse = new UserResponseDTO(1L, "Test User", "test@example.com", Role.USER, LocalDateTime.now());
        LoginResponseDTO loginResponse = new LoginResponseDTO("mocked.jwt.token", "Bearer", 86400000L, userResponse);

        when(authService.login(any(LoginRequestDTO.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        verify(authService, times(1)).login(any(LoginRequestDTO.class));
    }

    @Test
    void testLogin_InvalidCredentials_Returns401() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("test@example.com", "WrongPassword");

        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(authService, times(1)).login(any(LoginRequestDTO.class));
    }
}
