import axios from 'axios';

// Base URLs configured via environment variables
export const HOTEL_SERVICE_BASE_URL =
  import.meta.env.VITE_HOTEL_SERVICE_URL || 'http://localhost:8081';

export const BOOKING_SERVICE_BASE_URL =
  import.meta.env.VITE_BOOKING_SERVICE_URL || 'http://localhost:8082';

export const USER_SERVICE_BASE_URL =
  import.meta.env.VITE_USER_SERVICE_URL || 'http://localhost:8083';

// Axios client for Hotel Service
export const hotelClient = axios.create({
  baseURL: HOTEL_SERVICE_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 8000,
});

// Axios client for Booking Service
export const bookingClient = axios.create({
  baseURL: BOOKING_SERVICE_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 8000,
});

// Axios client for User Service
export const userClient = axios.create({
  baseURL: USER_SERVICE_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 8000,
});

// Request interceptor to automatically attach JWT Bearer token if available
const attachAuthToken = (config) => {
  try {
    const token = localStorage.getItem('grandstay_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  } catch {
    // ignore localStorage access issues
  }
  return config;
};

bookingClient.interceptors.request.use(attachAuthToken, (error) => Promise.reject(error));
userClient.interceptors.request.use(attachAuthToken, (error) => Promise.reject(error));
