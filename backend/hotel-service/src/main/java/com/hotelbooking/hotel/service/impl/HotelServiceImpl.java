package com.hotelbooking.hotel.service.impl;

import com.hotelbooking.hotel.dto.HotelRequestDTO;
import com.hotelbooking.hotel.dto.HotelResponseDTO;
import com.hotelbooking.hotel.dto.RoomResponseDTO;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.exception.ResourceNotFoundException;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.service.HotelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;

    public HotelServiceImpl(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override
    public HotelResponseDTO createHotel(HotelRequestDTO requestDTO) {
        Hotel hotel = new Hotel(
                requestDTO.getName(),
                requestDTO.getLocation(),
                requestDTO.getDescription(),
                requestDTO.getRating()
        );
        Hotel savedHotel = hotelRepository.save(hotel);
        return mapToResponseDTO(savedHotel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponseDTO> getAllHotels() {
        List<Hotel> hotels = hotelRepository.findAll();
        List<HotelResponseDTO> responseDTOs = new ArrayList<>();
        for (Hotel hotel : hotels) {
            responseDTOs.add(mapToResponseDTO(hotel));
        }
        return responseDTOs;
    }

    @Override
    @Transactional(readOnly = true)
    public HotelResponseDTO getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        return mapToResponseDTO(hotel);
    }

    @Override
    public HotelResponseDTO updateHotel(Long id, HotelRequestDTO requestDTO) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));

        hotel.setName(requestDTO.getName());
        hotel.setLocation(requestDTO.getLocation());
        hotel.setDescription(requestDTO.getDescription());
        hotel.setRating(requestDTO.getRating());

        Hotel updatedHotel = hotelRepository.save(hotel);
        return mapToResponseDTO(updatedHotel);
    }

    @Override
    public void deleteHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
        hotelRepository.delete(hotel);
    }

    private HotelResponseDTO mapToResponseDTO(Hotel hotel) {
        HotelResponseDTO dto = new HotelResponseDTO(
                hotel.getId(),
                hotel.getName(),
                hotel.getLocation(),
                hotel.getDescription(),
                hotel.getRating()
        );

        if (hotel.getRooms() != null) {
            List<RoomResponseDTO> roomDTOs = new ArrayList<>();
            for (Room room : hotel.getRooms()) {
                roomDTOs.add(mapRoomToResponseDTO(room, hotel.getId()));
            }
            dto.setRooms(roomDTOs);
        }

        return dto;
    }

    private RoomResponseDTO mapRoomToResponseDTO(Room room, Long hotelId) {
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
