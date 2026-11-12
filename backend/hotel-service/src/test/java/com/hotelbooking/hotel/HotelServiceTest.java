package com.hotelbooking.hotel;

import com.hotelbooking.hotel.dto.HotelRequestDTO;
import com.hotelbooking.hotel.dto.HotelResponseDTO;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.exception.ResourceNotFoundException;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.service.impl.HotelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelServiceImpl hotelService;

    private Hotel sampleHotel;
    private HotelRequestDTO sampleRequestDTO;

    @BeforeEach
    void setUp() {
        sampleHotel = new Hotel(1L, "Grand Palace", "New York", "Luxury Hotel", 4.8);
        sampleRequestDTO = new HotelRequestDTO("Grand Palace", "New York", "Luxury Hotel", 4.8);
    }

    @Test
    void testCreateHotel() {
        when(hotelRepository.save(any(Hotel.class))).thenReturn(sampleHotel);

        HotelResponseDTO response = hotelService.createHotel(sampleRequestDTO);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Grand Palace", response.getName());
        assertEquals("New York", response.getLocation());
        assertEquals(4.8, response.getRating());
        verify(hotelRepository, times(1)).save(any(Hotel.class));
    }

    @Test
    void testGetAllHotels() {
        Hotel secondHotel = new Hotel(2L, "Seaside Resort", "Miami", "Beachfront", 4.5);
        when(hotelRepository.findAll()).thenReturn(Arrays.asList(sampleHotel, secondHotel));

        List<HotelResponseDTO> hotels = hotelService.getAllHotels();

        assertEquals(2, hotels.size());
        assertEquals("Grand Palace", hotels.get(0).getName());
        assertEquals("Seaside Resort", hotels.get(1).getName());
        verify(hotelRepository, times(1)).findAll();
    }

    @Test
    void testGetHotelById_Success() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));

        HotelResponseDTO response = hotelService.getHotelById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Grand Palace", response.getName());
        verify(hotelRepository, times(1)).findById(1L);
    }

    @Test
    void testGetHotelById_NotFound() {
        when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> hotelService.getHotelById(99L));
        verify(hotelRepository, times(1)).findById(99L);
    }

    @Test
    void testUpdateHotel_Success() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        when(hotelRepository.save(any(Hotel.class))).thenReturn(sampleHotel);

        HotelRequestDTO updateRequest = new HotelRequestDTO("Grand Palace Updated", "New York City", "Renovated Luxury", 4.9);
        HotelResponseDTO updated = hotelService.updateHotel(1L, updateRequest);

        assertNotNull(updated);
        assertEquals("Grand Palace Updated", sampleHotel.getName());
        assertEquals(4.9, sampleHotel.getRating());
        verify(hotelRepository, times(1)).findById(1L);
        verify(hotelRepository, times(1)).save(sampleHotel);
    }

    @Test
    void testDeleteHotel_Success() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(sampleHotel));
        doNothing().when(hotelRepository).delete(sampleHotel);

        hotelService.deleteHotel(1L);

        verify(hotelRepository, times(1)).findById(1L);
        verify(hotelRepository, times(1)).delete(sampleHotel);
    }
}
