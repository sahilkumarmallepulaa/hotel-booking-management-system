package com.hotelbooking.hotel.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class HotelRequestDTO {

    @NotBlank(message = "Hotel name is required")
    private String name;

    @NotBlank(message = "Hotel location is required")
    private String location;

    private String description;

    @NotNull(message = "Hotel rating is required")
    @DecimalMin(value = "0.0", message = "Rating must be at least 0.0")
    @DecimalMax(value = "5.0", message = "Rating must not exceed 5.0")
    private Double rating;

    public HotelRequestDTO() {
    }

    public HotelRequestDTO(String name, String location, String description, Double rating) {
        this.name = name;
        this.location = location;
        this.description = description;
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
}
