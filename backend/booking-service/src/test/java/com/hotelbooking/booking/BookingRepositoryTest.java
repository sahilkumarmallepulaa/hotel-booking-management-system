package com.hotelbooking.booking;

import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    private final Long roomId = 101L;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();

        // Existing CONFIRMED booking from 2026-06-05 to 2026-06-10
        Booking existingBooking = new Booking(
                1L, 1L, roomId,
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 10),
                500.0,
                BookingStatus.CONFIRMED,
                LocalDateTime.now()
        );
        bookingRepository.save(existingBooking);
    }

    @Test
    void testOverlap_InsideRange_ReturnsTrue() {
        // 2026-06-06 to 2026-06-08 (entirely within existing booking)
        boolean overlap = bookingRepository.existsOverlappingBooking(
                roomId,
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 6, 8)
        );
        assertTrue(overlap, "Should detect overlap when new range is inside existing booking");
    }

    @Test
    void testOverlap_StartOverlap_ReturnsTrue() {
        // 2026-06-03 to 2026-06-07 (starts before, ends inside)
        boolean overlap = bookingRepository.existsOverlappingBooking(
                roomId,
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 6, 7)
        );
        assertTrue(overlap, "Should detect overlap when new range overlaps start of existing booking");
    }

    @Test
    void testOverlap_EndOverlap_ReturnsTrue() {
        // 2026-06-08 to 2026-06-12 (starts inside, ends after)
        boolean overlap = bookingRepository.existsOverlappingBooking(
                roomId,
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 6, 8),
                LocalDate.of(2026, 6, 12)
        );
        assertTrue(overlap, "Should detect overlap when new range overlaps end of existing booking");
    }

    @Test
    void testOverlap_EnclosingRange_ReturnsTrue() {
        // 2026-06-01 to 2026-06-15 (completely covers existing booking)
        boolean overlap = bookingRepository.existsOverlappingBooking(
                roomId,
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15)
        );
        assertTrue(overlap, "Should detect overlap when new range completely encloses existing booking");
    }

    @Test
    void testOverlap_AdjacentBefore_ReturnsFalse() {
        // 2026-06-01 to 2026-06-05 (check-out matches existing check-in: no overlap)
        boolean overlap = bookingRepository.existsOverlappingBooking(
                roomId,
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5)
        );
        assertFalse(overlap, "Adjacent booking ending when existing starts should NOT overlap");
    }

    @Test
    void testOverlap_AdjacentAfter_ReturnsFalse() {
        // 2026-06-10 to 2026-06-15 (check-in matches existing check-out: no overlap)
        boolean overlap = bookingRepository.existsOverlappingBooking(
                roomId,
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 15)
        );
        assertFalse(overlap, "Adjacent booking starting when existing ends should NOT overlap");
    }

    @Test
    void testOverlap_DifferentRoom_ReturnsFalse() {
        // Same dates but different roomId (202L)
        boolean overlap = bookingRepository.existsOverlappingBooking(
                202L,
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 6, 8)
        );
        assertFalse(overlap, "Overlap check should be scoped to the specific roomId");
    }

    @Test
    void testOverlap_CancelledBooking_Ignored() {
        // Cancel the existing booking
        Booking b = bookingRepository.findAll().get(0);
        b.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(b);

        boolean overlap = bookingRepository.existsOverlappingBooking(
                roomId,
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 6, 8)
        );
        assertFalse(overlap, "Cancelled bookings should not block new confirmed bookings");
    }
}
