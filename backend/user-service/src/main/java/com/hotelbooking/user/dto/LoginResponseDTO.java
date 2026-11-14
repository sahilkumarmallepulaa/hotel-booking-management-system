package com.hotelbooking.user.dto;

public class LoginResponseDTO {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private UserResponseDTO user;

    public LoginResponseDTO() {
        this.tokenType = "Bearer";
    }

    public LoginResponseDTO(String token, Long expiresIn, UserResponseDTO user) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public LoginResponseDTO(String token, String tokenType, Long expiresIn, UserResponseDTO user) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserResponseDTO getUser() {
        return user;
    }

    public void setUser(UserResponseDTO user) {
        this.user = user;
    }
}
