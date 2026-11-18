package com.hotelbooking.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotelbooking.booking.controller.BookingController;
import com.hotelbooking.booking.dto.BookingRequestDTO;
import com.hotelbooking.booking.dto.BookingResponseDTO;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.exception.BookingConflictException;
import com.hotelbooking.booking.exception.GlobalExceptionHandler;
import com.hotelbooking.booking.exception.ResourceNotFoundException;
import com.hotelbooking.booking.security.AuthenticatedUser;
import com.hotelbooking.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Resolver to simulate authenticated user ID 10 in standalone MVC tests
        HandlerMethodArgumentResolver authUserResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(AuthenticatedUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return new AuthenticatedUser(10L, "test@example.com", "USER");
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setCustomArgumentResolvers(authUserResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testCreateBooking_Success() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(4);
        BookingRequestDTO request = new BookingRequestDTO(10L, 1L, 101L, checkIn, checkOut);
        BookingResponseDTO response = new BookingResponseDTO(1L, 10L, 1L, 101L, checkIn, checkOut, 450.0, BookingStatus.CONFIRMED, LocalDateTime.now());

        when(bookingService.createBooking(any(BookingRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(10L))
                .andExpect(jsonPath("$.totalPrice").value(450.0))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }

    @Test
    void testCreateBooking_ValidationFailure() throws Exception {
        BookingRequestDTO invalidRequest = new BookingRequestDTO(null, null, null, null, null);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.userId").exists())
                .andExpect(jsonPath("$.validationErrors.hotelId").exists())
                .andExpect(jsonPath("$.validationErrors.roomId").exists())
                .andExpect(jsonPath("$.validationErrors.checkInDate").exists())
                .andExpect(jsonPath("$.validationErrors.checkOutDate").exists());

        verify(bookingService, never()).createBooking(any(BookingRequestDTO.class));
    }

    @Test
    void testCreateBooking_ConflictReturns409() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(4);
        BookingRequestDTO request = new BookingRequestDTO(10L, 1L, 101L, checkIn, checkOut);

        when(bookingService.createBooking(any(BookingRequestDTO.class)))
                .thenThrow(new BookingConflictException("Room is already booked for selected dates"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Room is already booked for selected dates"));

        verify(bookingService, times(1)).createBooking(any(BookingRequestDTO.class));
    }

    @Test
    void testGetBookingById_Success() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(4);
        BookingResponseDTO response = new BookingResponseDTO(1L, 10L, 1L, 101L, checkIn, checkOut, 450.0, BookingStatus.CONFIRMED, LocalDateTime.now());

        when(bookingService.getBookingById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.roomId").value(101L));

        verify(bookingService, times(1)).getBookingById(1L);
    }

    @Test
    void testGetBookingById_NotFoundReturns404() throws Exception {
        when(bookingService.getBookingById(99L)).thenThrow(new ResourceNotFoundException("Booking not found with id: 99"));

        mockMvc.perform(get("/api/bookings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found with id: 99"));

        verify(bookingService, times(1)).getBookingById(99L);
    }

    @Test
    void testGetBookingsByUserId_Success() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(4);
        BookingResponseDTO response = new BookingResponseDTO(1L, 10L, 1L, 101L, checkIn, checkOut, 450.0, BookingStatus.CONFIRMED, LocalDateTime.now());

        when(bookingService.getBookingsByUserId(10L)).thenReturn(Collections.singletonList(response));

        mockMvc.perform(get("/api/bookings/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(10L));

        verify(bookingService, times(1)).getBookingsByUserId(10L);
    }

    @Test
    void testCancelBooking_Success() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(4);
        BookingResponseDTO existingBooking = new BookingResponseDTO(1L, 10L, 1L, 101L, checkIn, checkOut, 450.0, BookingStatus.CONFIRMED, LocalDateTime.now());
        BookingResponseDTO cancelledResponse = new BookingResponseDTO(1L, 10L, 1L, 101L, checkIn, checkOut, 450.0, BookingStatus.CANCELLED, LocalDateTime.now());

        when(bookingService.getBookingById(1L)).thenReturn(existingBooking);
        when(bookingService.cancelBooking(1L)).thenReturn(cancelledResponse);

        mockMvc.perform(put("/api/bookings/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));

        verify(bookingService, times(1)).cancelBooking(1L);
    }
}
