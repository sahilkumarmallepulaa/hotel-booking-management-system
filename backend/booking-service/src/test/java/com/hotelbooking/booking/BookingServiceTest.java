package com.hotelbooking.booking;

import com.hotelbooking.booking.client.HotelServiceClient;
import com.hotelbooking.booking.client.dto.RoomClientDTO;
import com.hotelbooking.booking.dto.BookingRequestDTO;
import com.hotelbooking.booking.dto.BookingResponseDTO;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.exception.BookingConflictException;
import com.hotelbooking.booking.exception.InvalidBookingException;
import com.hotelbooking.booking.exception.ResourceNotFoundException;
import com.hotelbooking.booking.repository.BookingRepository;
import com.hotelbooking.booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelServiceClient hotelServiceClient;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private RoomClientDTO availableRoom;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        availableRoom = new RoomClientDTO(101L, "101", "DELUXE", 150.0, 2, true, 1L);
        sampleBooking = new Booking(
                1L, 10L, 1L, 101L,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(4),
                450.0,
                BookingStatus.CONFIRMED,
                LocalDateTime.now()
        );
    }

    @Test
    void testCreateBooking_Success() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(4); // 3 nights @ 150 = 450.0
        BookingRequestDTO request = new BookingRequestDTO(10L, 1L, 101L, checkIn, checkOut);

        when(hotelServiceClient.getRoomsByHotelId(1L)).thenReturn(Collections.singletonList(availableRoom));
        when(bookingRepository.existsOverlappingBooking(101L, BookingStatus.CONFIRMED, checkIn, checkOut)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        BookingResponseDTO response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getUserId());
        assertEquals(1L, response.getHotelId());
        assertEquals(101L, response.getRoomId());
        assertEquals(450.0, response.getTotalPrice());
        assertEquals(BookingStatus.CONFIRMED, response.getBookingStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_CheckInAfterCheckOut_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        BookingRequestDTO request = new BookingRequestDTO(10L, 1L, 101L, checkIn, checkOut);

        assertThrows(InvalidBookingException.class, () -> bookingService.createBooking(request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_CheckInSameAsCheckOut_ThrowsException() {
        LocalDate sameDay = LocalDate.now().plusDays(2);
        BookingRequestDTO request = new BookingRequestDTO(10L, 1L, 101L, sameDay, sameDay);

        assertThrows(InvalidBookingException.class, () -> bookingService.createBooking(request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_RoomNotFound_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        BookingRequestDTO request = new BookingRequestDTO(10L, 1L, 999L, checkIn, checkOut);

        when(hotelServiceClient.getRoomsByHotelId(1L)).thenReturn(Collections.singletonList(availableRoom));

        assertThrows(ResourceNotFoundException.class, () -> bookingService.createBooking(request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_RoomUnavailable_ThrowsException() {
        availableRoom.setAvailable(false);
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        BookingRequestDTO request = new BookingRequestDTO(10L, 1L, 101L, checkIn, checkOut);

        when(hotelServiceClient.getRoomsByHotelId(1L)).thenReturn(Collections.singletonList(availableRoom));

        assertThrows(InvalidBookingException.class, () -> bookingService.createBooking(request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_BackToBackBooking_Allowed() {
        LocalDate checkIn = LocalDate.now().plusDays(4);
        LocalDate checkOut = LocalDate.now().plusDays(7); // starts exactly when sampleBooking ends
        BookingRequestDTO request = new BookingRequestDTO(10L, 1L, 101L, checkIn, checkOut);

        Booking backToBackBooking = new Booking(
                2L, 10L, 1L, 101L,
                checkIn, checkOut, 450.0,
                BookingStatus.CONFIRMED, LocalDateTime.now()
        );

        when(hotelServiceClient.getRoomsByHotelId(1L)).thenReturn(Collections.singletonList(availableRoom));
        when(bookingRepository.existsOverlappingBooking(101L, BookingStatus.CONFIRMED, checkIn, checkOut)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(backToBackBooking);

        BookingResponseDTO response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals(BookingStatus.CONFIRMED, response.getBookingStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_OverlappingBookingExists_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(4);
        BookingRequestDTO request = new BookingRequestDTO(10L, 1L, 101L, checkIn, checkOut);

        when(hotelServiceClient.getRoomsByHotelId(1L)).thenReturn(Collections.singletonList(availableRoom));
        when(bookingRepository.existsOverlappingBooking(101L, BookingStatus.CONFIRMED, checkIn, checkOut)).thenReturn(true);

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testGetBookingById_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        BookingResponseDTO response = bookingService.getBookingById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(101L, response.getRoomId());
        verify(bookingRepository, times(1)).findById(1L);
    }

    @Test
    void testGetBookingById_NotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.getBookingById(99L));
        verify(bookingRepository, times(1)).findById(99L);
    }

    @Test
    void testGetBookingsByUserId_Success() {
        when(bookingRepository.findByUserId(10L)).thenReturn(Collections.singletonList(sampleBooking));

        List<BookingResponseDTO> list = bookingService.getBookingsByUserId(10L);

        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).getUserId());
        verify(bookingRepository, times(1)).findByUserId(10L);
    }

    @Test
    void testCancelBooking_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        BookingResponseDTO response = bookingService.cancelBooking(1L);

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, sampleBooking.getBookingStatus());
        verify(bookingRepository, times(1)).save(sampleBooking);
    }

    @Test
    void testCancelBooking_AlreadyCancelled_ThrowsException() {
        sampleBooking.setBookingStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        assertThrows(InvalidBookingException.class, () -> bookingService.cancelBooking(1L));
        verify(bookingRepository, never()).save(sampleBooking);
    }
}
