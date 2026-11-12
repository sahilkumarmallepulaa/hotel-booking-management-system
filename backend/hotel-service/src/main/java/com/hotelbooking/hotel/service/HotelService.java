package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.HotelRequestDTO;
import com.hotelbooking.hotel.dto.HotelResponseDTO;

import java.util.List;

public interface HotelService {
    HotelResponseDTO createHotel(HotelRequestDTO requestDTO);
    List<HotelResponseDTO> getAllHotels();
    HotelResponseDTO getHotelById(Long id);
    HotelResponseDTO updateHotel(Long id, HotelRequestDTO requestDTO);
    void deleteHotel(Long id);
}
