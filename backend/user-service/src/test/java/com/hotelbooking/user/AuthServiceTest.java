package com.hotelbooking.user;

import com.hotelbooking.user.dto.LoginRequestDTO;
import com.hotelbooking.user.dto.LoginResponseDTO;
import com.hotelbooking.user.dto.RegisterRequestDTO;
import com.hotelbooking.user.dto.UserResponseDTO;
import com.hotelbooking.user.entity.Role;
import com.hotelbooking.user.entity.User;
import com.hotelbooking.user.exception.InvalidCredentialsException;
import com.hotelbooking.user.exception.UserAlreadyExistsException;
import com.hotelbooking.user.repository.UserRepository;
import com.hotelbooking.user.security.JwtUtil;
import com.hotelbooking.user.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private final String rawPassword = "Password@123";
    private final String encodedPassword = "$2a$10$encodedPasswordHashExample";

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "Test User", "test@example.com", encodedPassword, Role.USER, LocalDateTime.now());
    }

    @Test
    void testRegister_Success() {
        RegisterRequestDTO request = new RegisterRequestDTO("Test User", "test@example.com", rawPassword);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponseDTO response = authService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
        assertNotNull(response.getCreatedAt());

        // Verify password was encrypted
        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegister_DuplicateEmail_ThrowsUserAlreadyExistsException() {
        RegisterRequestDTO request = new RegisterRequestDTO("Test User", "test@example.com", rawPassword);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        LoginRequestDTO request = new LoginRequestDTO("test@example.com", rawPassword);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com", "USER", 1L)).thenReturn("mocked.jwt.token");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400000L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals("test@example.com", response.getUser().getEmail());

        verify(passwordEncoder, times(1)).matches(rawPassword, encodedPassword);
    }

    @Test
    void testLogin_IncorrectPassword_ThrowsInvalidCredentialsException() {
        LoginRequestDTO request = new LoginRequestDTO("test@example.com", "WrongPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", encodedPassword)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyLong());
    }

    @Test
    void testLogin_NonExistentEmail_ThrowsInvalidCredentialsException() {
        LoginRequestDTO request = new LoginRequestDTO("nonexistent@example.com", rawPassword);

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
