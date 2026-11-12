package com.hotelbooking.booking.service.impl;

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
import com.hotelbooking.booking.service.BookingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final HotelServiceClient hotelServiceClient;

    public BookingServiceImpl(BookingRepository bookingRepository, HotelServiceClient hotelServiceClient) {
        this.bookingRepository = bookingRepository;
        this.hotelServiceClient = hotelServiceClient;
    }

    @Override
    public BookingResponseDTO createBooking(BookingRequestDTO requestDTO) {
        if (requestDTO.getCheckInDate() == null || requestDTO.getCheckOutDate() == null) {
            throw new InvalidBookingException("Check-in and check-out dates are required");
        }

        if (!requestDTO.getCheckInDate().isBefore(requestDTO.getCheckOutDate())) {
            throw new InvalidBookingException("Check-in date (" + requestDTO.getCheckInDate()
                    + ") must be strictly before check-out date (" + requestDTO.getCheckOutDate() + ")");
        }

        // Validate hotel and room existence via Feign client
        List<RoomClientDTO> rooms = hotelServiceClient.getRoomsByHotelId(requestDTO.getHotelId());
        if (rooms == null || rooms.isEmpty()) {
            throw new ResourceNotFoundException("No rooms found for hotel id: " + requestDTO.getHotelId());
        }

        RoomClientDTO targetRoom = null;
        for (RoomClientDTO room : rooms) {
            if (room.getId() != null && room.getId().equals(requestDTO.getRoomId())) {
                targetRoom = room;
                break;
            }
        }

        if (targetRoom == null) {
            throw new ResourceNotFoundException("Room id " + requestDTO.getRoomId() + " not found in hotel id " + requestDTO.getHotelId());
        }

        if (targetRoom.getAvailable() == null || !targetRoom.getAvailable()) {
            throw new InvalidBookingException("Room " + targetRoom.getRoomNumber() + " is currently not available for booking");
        }

        // Validate date overlap for the room with existing CONFIRMED bookings
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                requestDTO.getRoomId(),
                BookingStatus.CONFIRMED,
                requestDTO.getCheckInDate(),
                requestDTO.getCheckOutDate()
        );

        if (hasOverlap) {
            throw new BookingConflictException("Room " + requestDTO.getRoomId()
                    + " is already booked for the selected date range: "
                    + requestDTO.getCheckInDate() + " to " + requestDTO.getCheckOutDate());
        }

        long numberOfNights = ChronoUnit.DAYS.between(requestDTO.getCheckInDate(), requestDTO.getCheckOutDate());
        double totalPrice = targetRoom.getPricePerNight() * numberOfNights;

        Booking booking = new Booking(
                requestDTO.getUserId(),
                requestDTO.getHotelId(),
                requestDTO.getRoomId(),
                requestDTO.getCheckInDate(),
                requestDTO.getCheckOutDate(),
                totalPrice,
                BookingStatus.CONFIRMED,
                LocalDateTime.now()
        );

        Booking savedBooking = bookingRepository.save(booking);
        return mapToResponseDTO(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return mapToResponseDTO(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByUserId(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        List<BookingResponseDTO> responseDTOs = new ArrayList<>();
        for (Booking booking : bookings) {
            responseDTOs.add(mapToResponseDTO(booking));
        }
        return responseDTOs;
    }

    @Override
    public BookingResponseDTO cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("Booking id " + id + " is already cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.save(booking);
        return mapToResponseDTO(updatedBooking);
    }

    private BookingResponseDTO mapToResponseDTO(Booking booking) {
        return new BookingResponseDTO(
                booking.getId(),
                booking.getUserId(),
                booking.getHotelId(),
                booking.getRoomId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getTotalPrice(),
                booking.getBookingStatus(),
                booking.getCreatedAt()
        );
    }
}
