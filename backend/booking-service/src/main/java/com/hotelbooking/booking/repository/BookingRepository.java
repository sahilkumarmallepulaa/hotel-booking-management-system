package com.hotelbooking.booking.repository;

import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.roomId = :roomId " +
           "AND b.bookingStatus = :status " +
           "AND b.checkInDate < :checkOutDate " +
           "AND b.checkOutDate > :checkInDate")
    boolean existsOverlappingBooking(
            @Param("roomId") Long roomId,
            @Param("status") BookingStatus status,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate);
}
