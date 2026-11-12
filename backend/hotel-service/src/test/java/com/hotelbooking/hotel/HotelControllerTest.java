package com.hotelbooking.hotel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.hotel.controller.HotelController;
import com.hotelbooking.hotel.dto.HotelRequestDTO;
import com.hotelbooking.hotel.dto.HotelResponseDTO;
import com.hotelbooking.hotel.dto.RoomRequestDTO;
import com.hotelbooking.hotel.dto.RoomResponseDTO;
import com.hotelbooking.hotel.entity.RoomType;
import com.hotelbooking.hotel.exception.GlobalExceptionHandler;
import com.hotelbooking.hotel.exception.ResourceNotFoundException;
import com.hotelbooking.hotel.service.HotelService;
import com.hotelbooking.hotel.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HotelControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HotelService hotelService;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private HotelController hotelController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(hotelController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testCreateHotel_Success() throws Exception {
        HotelRequestDTO requestDTO = new HotelRequestDTO("Grand Palace", "New York", "Luxury Hotel", 4.8);
        HotelResponseDTO responseDTO = new HotelResponseDTO(1L, "Grand Palace", "New York", "Luxury Hotel", 4.8);

        when(hotelService.createHotel(any(HotelRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Grand Palace"))
                .andExpect(jsonPath("$.location").value("New York"))
                .andExpect(jsonPath("$.rating").value(4.8));

        verify(hotelService, times(1)).createHotel(any(HotelRequestDTO.class));
    }

    @Test
    void testCreateHotel_ValidationFailure() throws Exception {
        HotelRequestDTO invalidRequest = new HotelRequestDTO("", "", "Luxury Hotel", 6.0);

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.location").exists())
                .andExpect(jsonPath("$.validationErrors.rating").exists());

        verify(hotelService, never()).createHotel(any(HotelRequestDTO.class));
    }

    @Test
    void testGetAllHotels() throws Exception {
        HotelResponseDTO hotel1 = new HotelResponseDTO(1L, "Grand Palace", "New York", "Luxury Hotel", 4.8);
        HotelResponseDTO hotel2 = new HotelResponseDTO(2L, "Seaside Resort", "Miami", "Beachfront", 4.5);

        when(hotelService.getAllHotels()).thenReturn(Arrays.asList(hotel1, hotel2));

        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Grand Palace"))
                .andExpect(jsonPath("$[1].name").value("Seaside Resort"));

        verify(hotelService, times(1)).getAllHotels();
    }

    @Test
    void testGetHotelById_Success() throws Exception {
        HotelResponseDTO responseDTO = new HotelResponseDTO(1L, "Grand Palace", "New York", "Luxury Hotel", 4.8);
        when(hotelService.getHotelById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/hotels/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Grand Palace"));

        verify(hotelService, times(1)).getHotelById(1L);
    }

    @Test
    void testGetHotelById_NotFound() throws Exception {
        when(hotelService.getHotelById(99L)).thenThrow(new ResourceNotFoundException("Hotel not found with id: 99"));

        mockMvc.perform(get("/api/hotels/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Hotel not found with id: 99"));

        verify(hotelService, times(1)).getHotelById(99L);
    }

    @Test
    void testUpdateHotel_Success() throws Exception {
        HotelRequestDTO updateRequest = new HotelRequestDTO("Grand Palace Renovated", "New York", "Updated Luxury", 4.9);
        HotelResponseDTO responseDTO = new HotelResponseDTO(1L, "Grand Palace Renovated", "New York", "Updated Luxury", 4.9);

        when(hotelService.updateHotel(eq(1L), any(HotelRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/hotels/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grand Palace Renovated"))
                .andExpect(jsonPath("$.rating").value(4.9));

        verify(hotelService, times(1)).updateHotel(eq(1L), any(HotelRequestDTO.class));
    }

    @Test
    void testDeleteHotel_Success() throws Exception {
        doNothing().when(hotelService).deleteHotel(1L);

        mockMvc.perform(delete("/api/hotels/1"))
                .andExpect(status().isNoContent());

        verify(hotelService, times(1)).deleteHotel(1L);
    }

    @Test
    void testAddRoomToHotel_Success() throws Exception {
        RoomRequestDTO roomRequest = new RoomRequestDTO("101", RoomType.DELUXE, 200.0, 2, true);
        RoomResponseDTO roomResponse = new RoomResponseDTO(10L, "101", RoomType.DELUXE, 200.0, 2, true, 1L);

        when(roomService.addRoomToHotel(eq(1L), any(RoomRequestDTO.class))).thenReturn(roomResponse);

        mockMvc.perform(post("/api/hotels/1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.roomNumber").value("101"))
                .andExpect(jsonPath("$.roomType").value("DELUXE"))
                .andExpect(jsonPath("$.pricePerNight").value(200.0))
                .andExpect(jsonPath("$.hotelId").value(1L));

        verify(roomService, times(1)).addRoomToHotel(eq(1L), any(RoomRequestDTO.class));
    }

    @Test
    void testGetRoomsByHotelId_Success() throws Exception {
        RoomResponseDTO roomResponse = new RoomResponseDTO(10L, "101", RoomType.DELUXE, 200.0, 2, true, 1L);
        when(roomService.getRoomsByHotelId(1L)).thenReturn(Collections.singletonList(roomResponse));

        mockMvc.perform(get("/api/hotels/1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roomNumber").value("101"));

        verify(roomService, times(1)).getRoomsByHotelId(1L);
    }
}
