package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.RoomRequestDTO;
import com.hotelbooking.hotel.dto.RoomResponseDTO;

import java.util.List;

public interface RoomService {
    RoomResponseDTO addRoomToHotel(Long hotelId, RoomRequestDTO requestDTO);
    List<RoomResponseDTO> getRoomsByHotelId(Long hotelId);
}
