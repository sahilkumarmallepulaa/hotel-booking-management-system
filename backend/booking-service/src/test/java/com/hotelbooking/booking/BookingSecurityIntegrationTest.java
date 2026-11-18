package com.hotelbooking.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hotelbooking.booking.client.HotelServiceClient;
import com.hotelbooking.booking.client.dto.RoomClientDTO;
import com.hotelbooking.booking.dto.BookingRequestDTO;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.repository.BookingRepository;
import com.hotelbooking.booking.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class BookingSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private HotelServiceClient hotelServiceClient;

    private ObjectMapper objectMapper;

    private String userAToken;
    private String userBToken;
    private String adminToken;
    private String expiredToken;
    private Long userAId = 1001L;
    private Long userBId = 1002L;
    private Long adminId = 9999L;

    private Booking userABooking;
    private Booking userBBooking;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Generate tokens using the shared secret
        userAToken = jwtUtil.generateToken("userA@example.com", "USER", userAId, 86400000L);
        userBToken = jwtUtil.generateToken("userB@example.com", "USER", userBId, 86400000L);
        adminToken = jwtUtil.generateToken("admin@example.com", "ADMIN", adminId, 86400000L);
        // Expired token (-10 seconds)
        expiredToken = jwtUtil.generateToken("expired@example.com", "USER", userAId, -10000L);

        // Setup mock hotel service response for room creation
        RoomClientDTO room = new RoomClientDTO(501L, "501", "DELUXE", 200.0, 2, true, 10L);
        when(hotelServiceClient.getRoomsByHotelId(anyLong())).thenReturn(Collections.singletonList(room));

        // Create sample in-memory bookings for tests
        userABooking = bookingRepository.save(new Booking(
                userAId, 10L, 501L,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12),
                400.0, BookingStatus.CONFIRMED, LocalDateTime.now()
        ));

        userBBooking = bookingRepository.save(new Booking(
                userBId, 10L, 502L,
                LocalDate.now().plusDays(15), LocalDate.now().plusDays(18),
                600.0, BookingStatus.CONFIRMED, LocalDateTime.now()
        ));
    }

    // 1. Missing token returns 401
    @Test
    void testMissingToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/bookings/" + userABooking.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // 2. Invalid token returns 401
    @Test
    void testInvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/bookings/" + userABooking.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer InvalidTokenString12345"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // 3. Expired token returns 401
    @Test
    void testExpiredToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/bookings/" + userABooking.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // 4. Valid token can create a booking
    @Test
    void testValidToken_CanCreateBooking() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(25);
        LocalDate checkOut = LocalDate.now().plusDays(28);
        BookingRequestDTO request = new BookingRequestDTO(userAId, 10L, 501L, checkIn, checkOut);

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userAId))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPrice").value(600.0));
    }

    // 5. Booking creation cannot spoof another user's ID (server overrides with authenticated user ID)
    @Test
    void testBookingCreation_CannotSpoofOtherUserId() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(35);
        LocalDate checkOut = LocalDate.now().plusDays(38);
        // User A claims in the JSON body that userId is User B's ID (userBId)
        BookingRequestDTO spoofedRequest = new BookingRequestDTO(userBId, 10L, 501L, checkIn, checkOut);

        mockMvc.perform(post("/api/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(spoofedRequest)))
                .andExpect(status().isCreated())
                // Server must enforce userAId from the JWT, ignoring spoofed userBId
                .andExpect(jsonPath("$.userId").value(userAId));
    }

    // 6. User A cannot view User B's bookings list (returns 403)
    @Test
    void testUserA_CannotViewUserBBookings_Returns403() throws Exception {
        mockMvc.perform(get("/api/bookings/user/" + userBId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // 7. User A cannot view User B's individual booking (returns 403)
    @Test
    void testUserA_CannotViewUserBIndividualBooking_Returns403() throws Exception {
        mockMvc.perform(get("/api/bookings/" + userBBooking.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // 8. User A cannot cancel User B's booking (returns 403)
    @Test
    void testUserA_CannotCancelUserBBooking_Returns403() throws Exception {
        mockMvc.perform(put("/api/bookings/" + userBBooking.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // 9. User A can view and cancel their own booking (returns 200)
    @Test
    void testUserA_CanViewAndCancelOwnBooking() throws Exception {
        // View own booking
        mockMvc.perform(get("/api/bookings/" + userABooking.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userABooking.getId()))
                .andExpect(jsonPath("$.userId").value(userAId));

        // View own bookings list
        mockMvc.perform(get("/api/bookings/user/" + userAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userAId));

        // Cancel own booking
        mockMvc.perform(put("/api/bookings/" + userABooking.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));
    }

    // 10. Admin role can view other users' bookings
    @Test
    void testAdmin_CanViewAnyUserBookings() throws Exception {
        mockMvc.perform(get("/api/bookings/user/" + userAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bookings/" + userABooking.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userABooking.getId()));
    }
}
