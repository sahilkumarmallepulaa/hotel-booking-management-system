package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.HotelRequestDTO;
import com.hotelbooking.hotel.dto.HotelResponseDTO;
import com.hotelbooking.hotel.dto.RoomRequestDTO;
import com.hotelbooking.hotel.dto.RoomResponseDTO;
import com.hotelbooking.hotel.service.HotelService;
import com.hotelbooking.hotel.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;
    private final RoomService roomService;

    public HotelController(HotelService hotelService, RoomService roomService) {
        this.hotelService = hotelService;
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<HotelResponseDTO> createHotel(@Valid @RequestBody HotelRequestDTO requestDTO) {
        HotelResponseDTO createdHotel = hotelService.createHotel(requestDTO);
        return new ResponseEntity<>(createdHotel, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<HotelResponseDTO>> getAllHotels() {
        List<HotelResponseDTO> hotels = hotelService.getAllHotels();
        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelResponseDTO> getHotelById(@PathVariable("id") Long id) {
        HotelResponseDTO hotel = hotelService.getHotelById(id);
        return ResponseEntity.ok(hotel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HotelResponseDTO> updateHotel(
            @PathVariable("id") Long id,
            @Valid @RequestBody HotelRequestDTO requestDTO) {
        HotelResponseDTO updatedHotel = hotelService.updateHotel(id, requestDTO);
        return ResponseEntity.ok(updatedHotel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable("id") Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{hotelId}/rooms")
    public ResponseEntity<RoomResponseDTO> addRoomToHotel(
            @PathVariable("hotelId") Long hotelId,
            @Valid @RequestBody RoomRequestDTO requestDTO) {
        RoomResponseDTO createdRoom = roomService.addRoomToHotel(hotelId, requestDTO);
        return new ResponseEntity<>(createdRoom, HttpStatus.CREATED);
    }

    @GetMapping("/{hotelId}/rooms")
    public ResponseEntity<List<RoomResponseDTO>> getRoomsByHotelId(@PathVariable("hotelId") Long hotelId) {
        List<RoomResponseDTO> rooms = roomService.getRoomsByHotelId(hotelId);
        return ResponseEntity.ok(rooms);
    }
}
