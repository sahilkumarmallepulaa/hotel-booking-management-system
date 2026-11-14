package com.hotelbooking.user.service.impl;

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
import com.hotelbooking.user.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserResponseDTO register(RegisterRequestDTO requestDTO) {
        String normalizedEmail = requestDTO.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("Email is already registered: " + normalizedEmail);
        }

        String encodedPassword = passwordEncoder.encode(requestDTO.getPassword());

        User user = new User(
                requestDTO.getName().trim(),
                normalizedEmail,
                encodedPassword,
                Role.USER,
                LocalDateTime.now()
        );

        User savedUser = userRepository.save(user);

        return mapToUserResponseDTO(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO requestDTO) {
        String normalizedEmail = requestDTO.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        UserResponseDTO userResponseDTO = mapToUserResponseDTO(user);

        return new LoginResponseDTO(token, "Bearer", jwtUtil.getExpirationMs(), userResponseDTO);
    }

    private UserResponseDTO mapToUserResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
