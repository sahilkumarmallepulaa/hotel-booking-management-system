package com.hotelbooking.booking.client;

import com.hotelbooking.booking.client.dto.RoomClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "hotel-service")
public interface HotelServiceClient {

    @GetMapping("/api/hotels/{hotelId}/rooms")
    List<RoomClientDTO> getRoomsByHotelId(@PathVariable("hotelId") Long hotelId);
}
