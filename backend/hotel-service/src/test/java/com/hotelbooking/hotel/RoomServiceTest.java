package com.hotelbooking.hotel;

import com.hotelbooking.hotel.dto.RoomRequestDTO;
import com.hotelbooking.hotel.dto.RoomResponseDTO;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.RoomType;
import com.hotelbooking.hotel.exception.ResourceNotFoundException;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import com.hotelbooking.hotel.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Hotel sampleHotel;
    private Room sampleRoom;
    private RoomRequestDTO sampleRoomRequest;

    @BeforeEach
    void setUp() {
        sampleHotel = new Hotel(1L, "Grand Palace", "New York", "Luxury Hotel", 4.8);
        sampleRoom = new Room(101L, "101", RoomType.DELUXE, 250.0, 2, true, sampleHotel);
        sampleRoomRequest = new RoomRequestDTO("101", RoomType.DELUXE, 250.0, 2, true);
    }

    @Test
    void testAddRoomToHotel_Success() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);

        RoomResponseDTO response = roomService.addRoomToHotel(1L, sampleRoomRequest);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("101", response.getRoomNumber());
        assertEquals(RoomType.DELUXE, response.getRoomType());
        assertEquals(250.0, response.getPricePerNight());
        assertEquals(1L, response.getHotelId());
        verify(hotelRepository, times(1)).findById(1L);
        verify(roomRepository, times(1)).save(any(Room.class));
    }

    @Test
    void testAddRoomToHotel_HotelNotFound() {
        when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roomService.addRoomToHotel(99L, sampleRoomRequest));
        verify(hotelRepository, times(1)).findById(99L);
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void testGetRoomsByHotelId_Success() {
        when(hotelRepository.existsById(1L)).thenReturn(true);
        when(roomRepository.findByHotelId(1L)).thenReturn(Collections.singletonList(sampleRoom));

        List<RoomResponseDTO> rooms = roomService.getRoomsByHotelId(1L);

        assertEquals(1, rooms.size());
        assertEquals("101", rooms.get(0).getRoomNumber());
        assertEquals(RoomType.DELUXE, rooms.get(0).getRoomType());
        verify(hotelRepository, times(1)).existsById(1L);
        verify(roomRepository, times(1)).findByHotelId(1L);
    }

    @Test
    void testGetRoomsByHotelId_HotelNotFound() {
        when(hotelRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> roomService.getRoomsByHotelId(99L));
        verify(hotelRepository, times(1)).existsById(99L);
        verify(roomRepository, never()).findByHotelId(anyLong());
    }
}
