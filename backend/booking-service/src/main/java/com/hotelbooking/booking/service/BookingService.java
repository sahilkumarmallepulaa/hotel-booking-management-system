package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.BookingRequestDTO;
import com.hotelbooking.booking.dto.BookingResponseDTO;

import java.util.List;

public interface BookingService {
    BookingResponseDTO createBooking(BookingRequestDTO requestDTO);
    BookingResponseDTO getBookingById(Long id);
    List<BookingResponseDTO> getBookingsByUserId(Long userId);
    BookingResponseDTO cancelBooking(Long id);
}
