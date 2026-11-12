package com.hotelbooking.hotel.dto;

import java.util.ArrayList;
import java.util.List;

public class HotelResponseDTO {

    private Long id;
    private String name;
    private String location;
    private String description;
    private Double rating;
    private List<RoomResponseDTO> rooms = new ArrayList<>();

    public HotelResponseDTO() {
    }

    public HotelResponseDTO(Long id, String name, String location, String description, Double rating) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.rating = rating;
    }

    public HotelResponseDTO(Long id, String name, String location, String description, Double rating, List<RoomResponseDTO> rooms) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.rating = rating;
        this.rooms = (rooms != null) ? rooms : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<RoomResponseDTO> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomResponseDTO> rooms) {
        this.rooms = rooms;
    }
}
