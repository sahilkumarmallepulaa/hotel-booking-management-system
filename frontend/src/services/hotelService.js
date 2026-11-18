import { hotelClient } from './api';

export const hotelService = {
  // Fetch all hotels
  getAllHotels: async () => {
    const response = await hotelClient.get('/api/hotels');
    return response.data;
  },

  // Fetch hotel by ID
  getHotelById: async (id) => {
    const response = await hotelClient.get(`/api/hotels/${id}`);
    return response.data;
  },

  // Fetch all rooms for a hotel
  getHotelRooms: async (hotelId) => {
    const response = await hotelClient.get(`/api/hotels/${hotelId}/rooms`);
    return response.data;
  },
};

export default hotelService;
