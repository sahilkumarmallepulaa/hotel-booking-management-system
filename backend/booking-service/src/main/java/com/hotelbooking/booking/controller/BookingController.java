package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.dto.BookingRequestDTO;
import com.hotelbooking.booking.dto.BookingResponseDTO;
import com.hotelbooking.booking.security.AuthenticatedUser;
import com.hotelbooking.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO requestDTO,
            @AuthenticationPrincipal AuthenticatedUser user) {
        // Enforce server-side security: derive userId from the verified JWT token
        // Do not trust the userId supplied in the frontend request body
        if (user != null && user.getUserId() != null) {
            requestDTO.setUserId(user.getUserId());
        }

        BookingResponseDTO createdBooking = bookingService.createBooking(requestDTO);
        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        BookingResponseDTO booking = bookingService.getBookingById(id);

        // Verify booking ownership: normal users can only view their own bookings
        if (user == null || (!user.isAdmin() && !booking.getUserId().equals(user.getUserId()))) {
            throw new AccessDeniedException("You do not have permission to view booking ID: " + id);
        }

        return ResponseEntity.ok(booking);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUserId(
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        // Verify booking ownership: normal users can only query their own user ID
        if (user == null || (!user.isAdmin() && !userId.equals(user.getUserId()))) {
            throw new AccessDeniedException("You do not have permission to access bookings for user ID: " + userId);
        }

        List<BookingResponseDTO> bookings = bookingService.getBookingsByUserId(userId);
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        BookingResponseDTO booking = bookingService.getBookingById(id);

        // Verify booking ownership before cancellation
        if (user == null || (!user.isAdmin() && !booking.getUserId().equals(user.getUserId()))) {
            throw new AccessDeniedException("You do not have permission to cancel booking ID: " + id);
        }

        BookingResponseDTO cancelledBooking = bookingService.cancelBooking(id);
        return ResponseEntity.ok(cancelledBooking);
    }
}
