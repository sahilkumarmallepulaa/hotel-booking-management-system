import { bookingClient } from './api';

/**
 * Service for interacting with Booking Service (port 8082)
 */
const bookingService = {
  /**
   * Create a new booking
   * POST /api/bookings
   * Request: { userId, hotelId, roomId, checkInDate, checkOutDate }
   * Response: BookingResponseDTO { id, userId, hotelId, roomId, checkInDate, checkOutDate, totalPrice, bookingStatus, createdAt }
   */
  createBooking: async (bookingData) => {
    const response = await bookingClient.post('/api/bookings', bookingData);
    return response.data;
  },

  /**
   * Get booking details by ID
   * GET /api/bookings/{id}
   */
  getBookingById: async (id) => {
    const response = await bookingClient.get(`/api/bookings/${id}`);
    return response.data;
  },

  /**
   * Get all bookings for a specific user
   * GET /api/bookings/user/{userId}
   */
  getBookingsByUserId: async (userId) => {
    const response = await bookingClient.get(`/api/bookings/user/${userId}`);
    return response.data;
  },

  /**
   * Cancel an existing booking
   * PUT /api/bookings/{id}/cancel
   */
  cancelBooking: async (id) => {
    const response = await bookingClient.put(`/api/bookings/${id}/cancel`);
    return response.data;
  },
};

export default bookingService;
