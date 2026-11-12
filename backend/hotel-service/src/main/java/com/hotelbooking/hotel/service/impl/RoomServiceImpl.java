package com.hotelbooking.hotel.service.impl;

import com.hotelbooking.hotel.dto.RoomRequestDTO;
import com.hotelbooking.hotel.dto.RoomResponseDTO;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.exception.ResourceNotFoundException;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import com.hotelbooking.hotel.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    public RoomServiceImpl(RoomRepository roomRepository, HotelRepository hotelRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
    }

    @Override
    public RoomResponseDTO addRoomToHotel(Long hotelId, RoomRequestDTO requestDTO) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + hotelId));

        Room room = new Room(
                requestDTO.getRoomNumber(),
                requestDTO.getRoomType(),
                requestDTO.getPricePerNight(),
                requestDTO.getCapacity(),
                requestDTO.getAvailable()
        );
        hotel.addRoom(room);

        Room savedRoom = roomRepository.save(room);
        return mapToResponseDTO(savedRoom, hotelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getRoomsByHotelId(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException("Hotel not found with id: " + hotelId);
        }

        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        List<RoomResponseDTO> responseDTOs = new ArrayList<>();
        for (Room room : rooms) {
            responseDTOs.add(mapToResponseDTO(room, hotelId));
        }
        return responseDTOs;
    }

    private RoomResponseDTO mapToResponseDTO(Room room, Long hotelId) {
        return new RoomResponseDTO(
                room.getId(),
                room.getRoomNumber(),
                room.getRoomType(),
                room.getPricePerNight(),
                room.getCapacity(),
                room.getAvailable(),
                hotelId
        );
    }
}
