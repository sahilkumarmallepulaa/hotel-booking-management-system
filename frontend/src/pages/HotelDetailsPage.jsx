import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useParams, Link, useNavigate, useLocation } from 'react-router-dom';
import hotelService from '../services/hotelService';
import bookingService from '../services/bookingService';
import { useAuth } from '../context/AuthContext';
import { formatINR } from '../utils/formatters';

const HOTEL_IMAGES = [
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1200&q=80',
];

const ROOM_IMAGES = [
  'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=600&q=80',
];

// Helper to format date object to YYYY-MM-DD in local time
const formatDateToISO = (d) => {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const HotelDetailsPage = () => {
  const { hotelId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { currentUser, isAuthenticated } = useAuth();

  const [hotel, setHotel] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notFound, setNotFound] = useState(false);

  // Booking Modal State
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [checkInDate, setCheckInDate] = useState('');
  const [checkOutDate, setCheckOutDate] = useState('');
  const [isSubmittingBooking, setIsSubmittingBooking] = useState(false);
  const [bookingError, setBookingError] = useState(null);
  const [confirmedBooking, setConfirmedBooking] = useState(null);

  // Today and tomorrow dates for constraints
  const todayStr = useMemo(() => formatDateToISO(new Date()), []);
  const tomorrowStr = useMemo(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return formatDateToISO(d);
  }, []);

  const fetchHotel = useCallback(async () => {
    if (!hotelId) return;
    setLoading(true);
    setError(null);
    setNotFound(false);

    try {
      const data = await hotelService.getHotelById(hotelId);
      if (data && data.id) {
        setHotel(data);
      } else {
        setNotFound(true);
      }
    } catch (err) {
      console.error('Failed to load hotel details:', err);
      if (err.response && err.response.status === 404) {
        setNotFound(true);
      } else {
        const message =
          err.response?.data?.message ||
          err.message ||
          'Failed to connect to Hotel Service on port 8081.';
        setError(message);
      }
    } finally {
      setLoading(false);
    }
  }, [hotelId]);

  useEffect(() => {
    fetchHotel();
  }, [fetchHotel]);

  // Close modal on Escape key
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && selectedRoom && !isSubmittingBooking) {
        handleCloseModal();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectedRoom, isSubmittingBooking]);

  // Open booking modal or redirect unauthenticated user to login
  const handleOpenBooking = (room) => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      return;
    }

    // Default dates: today and tomorrow
    setCheckInDate(todayStr);
    setCheckOutDate(tomorrowStr);
    setBookingError(null);
    setConfirmedBooking(null);
    setSelectedRoom(room);
  };

  const handleCloseModal = () => {
    setSelectedRoom(null);
    setConfirmedBooking(null);
    setBookingError(null);
  };

  // Dynamic night calculation
  const calculateNights = (inDate, outDate) => {
    if (!inDate || !outDate) return 0;
    const d1 = new Date(inDate);
    const d2 = new Date(outDate);
    const diffTime = d2 - d1;
    return Math.round(diffTime / (1000 * 60 * 60 * 24));
  };

  const nightsCount = useMemo(() => {
    return calculateNights(checkInDate, checkOutDate);
  }, [checkInDate, checkOutDate]);

  const estimatedTotal = useMemo(() => {
    if (!selectedRoom || nightsCount <= 0) return 0;
    return (selectedRoom.pricePerNight || 0) * nightsCount;
  }, [selectedRoom, nightsCount]);

  // Minimum checkout date based on selected check-in date
  const minCheckOutDate = useMemo(() => {
    if (!checkInDate) return tomorrowStr;
    const parts = checkInDate.split('-').map(Number);
    if (parts.length === 3) {
      const d = new Date(parts[0], parts[1] - 1, parts[2]);
      d.setDate(d.getDate() + 1);
      return formatDateToISO(d);
    }
    return tomorrowStr;
  }, [checkInDate, tomorrowStr]);

  // Submit booking creation to Booking Service
  const handleBookingSubmit = async (e) => {
    e.preventDefault();
    if (isSubmittingBooking) return;

    setBookingError(null);

    if (!checkInDate || !checkOutDate) {
      setBookingError('Please specify both check-in and check-out dates.');
      return;
    }

    if (nightsCount <= 0) {
      setBookingError('Check-out date must be after check-in date (at least 1 night stay).');
      return;
    }

    if (checkInDate < todayStr) {
      setBookingError('Check-in date cannot be in the past.');
      return;
    }

    setIsSubmittingBooking(true);

    try {
      // STRICT BookingRequestDTO payload:
      // userId, hotelId, roomId, checkInDate, checkOutDate
      // Do NOT send totalPrice or pricePerNight (server calculates)
      const bookingPayload = {
        userId: currentUser?.id,
        hotelId: hotel.id,
        roomId: selectedRoom.id,
        checkInDate: checkInDate,
        checkOutDate: checkOutDate,
      };

      const response = await bookingService.createBooking(bookingPayload);
      setConfirmedBooking(response);
    } catch (err) {
      console.error('Booking submission failed:', err);
      if (err.response) {
        if (err.response.status === 409) {
          setBookingError(
            err.response.data?.message ||
              'This room is already booked for the selected dates. Please select different dates.'
          );
        } else if (err.response.status === 400) {
          setBookingError(
            err.response.data?.message ||
              'Invalid booking request. Please check the dates and try again.'
          );
        } else if (err.response.status === 404) {
          setBookingError(
            err.response.data?.message ||
              'The requested hotel, room, or user was not found.'
          );
        } else {
          setBookingError(
            err.response.data?.message ||
              `Booking failed with status ${err.response.status}. Please try again.`
          );
        }
      } else {
        setBookingError(
          'Unable to reach Booking Service (port 8082). Please verify the service is running.'
        );
      }
    } finally {
      setIsSubmittingBooking(false);
    }
  };

  // Loading State
  if (loading) {
    return (
      <div className="details-page">
        <div className="state-container" role="status" aria-live="polite">
          <div className="loading-box">
            <progress aria-label="Loading hotel details" className="loading-spinner"></progress>
            <div className="state-title">Loading Hotel Information</div>
            <div className="state-desc">
              Retrieving live hotel details and room inventory from Hotel Service (port 8081)...
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Not Found State
  if (notFound) {
    return (
      <div className="details-page">
        <div className="state-container state-empty" role="alert">
          <div className="state-icon">🔍</div>
          <h2 className="state-title">Hotel Not Found</h2>
          <p className="state-desc">
            We could not find a hotel matching ID #{hotelId}. It may have been removed or the ID is invalid.
          </p>
          <Link to="/hotels" className="btn-state-action">
            &larr; Browse All Hotels
          </Link>
        </div>
      </div>
    );
  }

  // Error State
  if (error) {
    return (
      <div className="details-page">
        <div className="state-container state-error" role="alert">
          <div className="state-icon">⚠️</div>
          <h2 className="state-title">Unable to Load Hotel</h2>
          <p className="state-desc">
            An error occurred while fetching hotel details from <code>GET /api/hotels/{hotelId}</code>:
            <br />
            <strong>{error}</strong>
          </p>
          <button type="button" className="btn-state-action btn-state-retry" onClick={fetchHotel}>
            🔄 Retry Connection
          </button>
        </div>
      </div>
    );
  }

  if (!hotel) return null;

  const rooms = Array.isArray(hotel.rooms) ? hotel.rooms : [];
  const hotelImage = HOTEL_IMAGES[(hotel.id || 0) % HOTEL_IMAGES.length];
  const lowestPrice =
    rooms.length > 0
      ? Math.min(...rooms.map((r) => r.pricePerNight || 0))
      : null;

  return (
    <>
      {/* Breadcrumb Navigation */}
      <nav className="breadcrumb-nav" aria-label="Breadcrumb">
        <Link to="/" className="breadcrumb-link">
          Home
        </Link>
        <span className="breadcrumb-sep">/</span>
        <Link to="/hotels" className="breadcrumb-link">
          Hotels
        </Link>
        <span className="breadcrumb-sep">/</span>
        <span className="breadcrumb-current">{hotel.name}</span>
      </nav>

      <div className="details-page">
        {/* Header Section */}
        <header className="details-header">
          <div className="details-header-info">
            <div className="details-title-row">
              <h1 className="details-hotel-name">{hotel.name}</h1>
              <div className="details-rating-chip">
                <span>★</span>
                <span>
                  {typeof hotel.rating === 'number'
                    ? hotel.rating.toFixed(1)
                    : '5.0'}
                </span>
                <span style={{ fontWeight: 400, color: '#b45309' }}>/ 5.0</span>
              </div>
            </div>

            <div className="details-location-row">
              <span>📍 {hotel.location}</span>
              <span>&bull;</span>
              <span>Luxury Accommodations</span>
              <span>&bull;</span>
              <span className="notice-badge notice-live">
                🟢 Live Data (Hotel ID: {hotel.id})
              </span>
            </div>
          </div>

          {lowestPrice !== null && (
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Starting from</div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--primary)' }}>
                {formatINR(lowestPrice)}
              </div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>per room / night</div>
            </div>
          )}
        </header>

        {/* Two-Column Grid Layout */}
        <div className="details-grid">
          {/* Main Left Column */}
          <main className="details-main-content">
            {/* Gallery Image */}
            <div className="hotel-gallery">
              <img
                src={hotelImage}
                alt={`${hotel.name} exterior and architecture`}
                className="hotel-gallery-img"
              />
            </div>

            {/* About the Hotel */}
            <section className="details-section-card">
              <h2 className="details-section-heading">
                <span>🏨</span>
                <span>About this Property</span>
              </h2>
              <p className="details-description-text">
                {hotel.description ||
                  'Experience bespoke luxury, personalized hospitality, and breathtaking views at this distinguished destination.'}
              </p>

              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginTop: '1.8rem', color: 'var(--text-main)' }}>
                Property Amenities & Inclusions
              </h3>
              <div className="amenities-grid">
                <div className="amenity-chip">
                  <span>📶</span>
                  <span>High-Speed Wi-Fi</span>
                </div>
                <div className="amenity-chip">
                  <span>🏊</span>
                  <span>Swimming Pool</span>
                </div>
                <div className="amenity-chip">
                  <span>❄️</span>
                  <span>Climate Control</span>
                </div>
                <div className="amenity-chip">
                  <span>🛎️</span>
                  <span>24/7 Front Desk</span>
                </div>
                <div className="amenity-chip">
                  <span>🍽️</span>
                  <span>Fine Dining</span>
                </div>
                <div className="amenity-chip">
                  <span>🚗</span>
                  <span>Valet Parking</span>
                </div>
              </div>
            </section>

            {/* Rooms Section */}
            <section className="details-section-card" id="rooms">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h2 className="details-section-heading" style={{ marginBottom: 0 }}>
                  <span>🛏️</span>
                  <span>Available Rooms & Suites</span>
                </h2>
                <span className="notice-badge notice-live">
                  {rooms.length} Room Type{rooms.length === 1 ? '' : 's'} Configured
                </span>
              </div>

              {/* Empty Rooms State */}
              {rooms.length === 0 ? (
                <div className="state-container state-empty" style={{ margin: '1.5rem 0 0' }}>
                  <div className="state-icon">🛋️</div>
                  <h3 className="state-title">No Rooms Currently Listed</h3>
                  <p className="state-desc">
                    This hotel does not have any rooms added to the inventory yet.
                  </p>
                </div>
              ) : (
                <div className="rooms-container">
                  {rooms.map((room, idx) => {
                    const roomImg = ROOM_IMAGES[idx % ROOM_IMAGES.length];
                    const isAvailable = room.available === true;

                    return (
                      <article key={room.id || idx} className="room-item-card">
                        <div className="room-img-wrapper">
                          <img
                            src={roomImg}
                            alt={`${room.roomType || 'Hotel'} Room ${room.roomNumber || ''}`}
                            className="room-img"
                            loading="lazy"
                          />
                        </div>

                        <div className="room-details-col">
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexWrap: 'wrap' }}>
                            <h3 className="room-type-title">
                              {room.roomType || 'Standard'} Room
                            </h3>
                            {room.roomNumber && (
                              <span className="room-number-tag">
                                Room #{room.roomNumber}
                              </span>
                            )}
                          </div>

                          <div className="room-specs">
                            <span>👥 Up to {room.capacity || 2} Guests</span>
                            <span>&bull;</span>
                            <span>👑 King / Queen Bed</span>
                            <span>&bull;</span>
                            <span>🚿 Ensuite Bath</span>
                          </div>

                          <div>
                            {isAvailable ? (
                              <span className="room-status-badge room-available-badge">
                                🟢 Available for Booking
                              </span>
                            ) : (
                              <span className="room-status-badge room-unavailable-badge">
                                🔴 Currently Unavailable
                              </span>
                            )}
                          </div>
                        </div>

                        <div className="room-action-col">
                          <div className="room-price-val">
                            {formatINR(room.pricePerNight)}
                          </div>
                          <div className="room-price-sub">per night + taxes</div>

                          {/* Book Now Button */}
                          {isAvailable ? (
                            <button
                              type="button"
                              className="btn-book-room"
                              onClick={() => handleOpenBooking(room)}
                            >
                              Book Now &rarr;
                            </button>
                          ) : (
                            <div className="room-sold-out-box">
                              Sold Out
                            </div>
                          )}
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>
          </main>

          {/* Right Sidebar Column */}
          <aside className="details-sidebar">
            <div className="sidebar-card">
              <h3 className="sidebar-title">Stay Policies</h3>
              <div className="policy-list">
                <div className="policy-item">
                  <span className="policy-label">Check-in</span>
                  <span className="policy-value">2:00 PM</span>
                </div>
                <div className="policy-item">
                  <span className="policy-label">Check-out</span>
                  <span className="policy-value">11:00 AM</span>
                </div>
                <div className="policy-item">
                  <span className="policy-label">Cancellation</span>
                  <span className="policy-value" style={{ color: '#059669' }}>
                    Free cancellation
                  </span>
                </div>
                <div className="policy-item">
                  <span className="policy-label">Payment</span>
                  <span className="policy-value">Pay upon arrival</span>
                </div>
              </div>
            </div>

            <div className="sidebar-card" style={{ background: '#f8fafc' }}>
              <h3 className="sidebar-title">GrandStay Guarantee</h3>
              <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.6, marginBottom: '1rem' }}>
                All rooms are independently inspected and verified through our microservices architecture for guaranteed availability.
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', fontSize: '0.85rem', fontWeight: 600 }}>
                <div>🛡️ Instant Confirmation</div>
                <div>✨ Zero Booking Surcharges</div>
                <div>🛎️ 24/7 Concierge Support</div>
              </div>
            </div>
          </aside>
        </div>
      </div>

      {/* Booking Modal Dialog */}
      {selectedRoom && (
        <div
          className="modal-overlay"
          onClick={() => {
            if (!isSubmittingBooking) handleCloseModal();
          }}
        >
          <div
            className="modal-card booking-modal-card"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="booking-modal-title"
          >
            {/* If booking confirmed, show confirmation receipt */}
            {confirmedBooking ? (
              <div className="booking-confirmed-view">
                <div className="confirmed-icon-circle">✓</div>
                <h2 id="booking-modal-title" className="confirmed-title">
                  Booking Confirmed!
                </h2>
                <p className="confirmed-subtitle">
                  Your reservation has been confirmed and registered with Booking Service.
                </p>

                <div className="confirmed-receipt">
                  <div className="receipt-header-row">
                    <div>
                      <span className="receipt-ref-label">Booking Reference</span>
                      <div className="receipt-ref-value">#{confirmedBooking.id}</div>
                    </div>
                    <div>
                      <span className="receipt-status-badge">
                        {confirmedBooking.bookingStatus || 'CONFIRMED'}
                      </span>
                    </div>
                  </div>

                  <div className="receipt-divider"></div>

                  <div className="receipt-grid">
                    <div className="receipt-item">
                      <span className="receipt-item-label">Property</span>
                      <strong className="receipt-item-value">{hotel.name}</strong>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>📍 {hotel.location}</span>
                    </div>

                    <div className="receipt-item">
                      <span className="receipt-item-label">Room</span>
                      <strong className="receipt-item-value">
                        {selectedRoom.roomType} Room (Room #{selectedRoom.roomNumber})
                      </strong>
                    </div>

                    <div className="receipt-item">
                      <span className="receipt-item-label">Guest</span>
                      <strong className="receipt-item-value">
                        {currentUser?.name || currentUser?.email}
                      </strong>
                    </div>

                    <div className="receipt-item">
                      <span className="receipt-item-label">Duration</span>
                      <strong className="receipt-item-value">
                        {calculateNights(confirmedBooking.checkInDate, confirmedBooking.checkOutDate)} Night(s)
                      </strong>
                    </div>

                    <div className="receipt-item">
                      <span className="receipt-item-label">Check-in Date</span>
                      <strong className="receipt-item-value">{confirmedBooking.checkInDate}</strong>
                    </div>

                    <div className="receipt-item">
                      <span className="receipt-item-label">Check-out Date</span>
                      <strong className="receipt-item-value">{confirmedBooking.checkOutDate}</strong>
                    </div>
                  </div>

                  <div className="receipt-divider"></div>

                  <div className="receipt-total-row">
                    <div className="receipt-total-label">
                      <span>Total Amount Paid / Due</span>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block' }}>
                        Calculated server-side by Booking Service
                      </span>
                    </div>
                    <div className="receipt-total-price">
                      {formatINR(confirmedBooking.totalPrice)}
                    </div>
                  </div>
                </div>

                <div className="modal-actions-row">
                  <button
                    type="button"
                    className="btn-modal-close"
                    onClick={handleCloseModal}
                  >
                    Done & Close
                  </button>
                </div>
              </div>
            ) : (
              /* Booking Creation Form */
              <div className="booking-form-view">
                <div className="modal-header">
                  <span className="modal-icon">🏨</span>
                  <div>
                    <h2 id="booking-modal-title" className="modal-title">
                      Reserve Your Room
                    </h2>
                    <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>
                      {hotel.name} &bull; {selectedRoom.roomType} Room (Room #{selectedRoom.roomNumber})
                    </p>
                  </div>
                </div>

                {/* Selected Room Specs Preview */}
                <div className="modal-info-box">
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
                    <span>Rate per night:</span>
                    <strong>{formatINR(selectedRoom.pricePerNight)}</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
                    <span>Max Capacity:</span>
                    <strong>{selectedRoom.capacity} Guests</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span>Guest Account:</span>
                    <strong>{currentUser?.name} ({currentUser?.email})</strong>
                  </div>
                </div>

                {/* Error Banner */}
                {bookingError && (
                  <div className="modal-error-banner" role="alert">
                    <span>⚠️</span>
                    <span>{bookingError}</span>
                  </div>
                )}

                <form onSubmit={handleBookingSubmit} className="booking-modal-form">
                  <div className="modal-dates-row">
                    <div className="form-group" style={{ flex: 1 }}>
                      <label htmlFor="modal-check-in" className="form-label">
                        Check-in Date
                      </label>
                      <input
                        id="modal-check-in"
                        type="date"
                        className="form-input"
                        value={checkInDate}
                        min={todayStr}
                        onChange={(e) => setCheckInDate(e.target.value)}
                        disabled={isSubmittingBooking}
                        required
                      />
                    </div>

                    <div className="form-group" style={{ flex: 1 }}>
                      <label htmlFor="modal-check-out" className="form-label">
                        Check-out Date
                      </label>
                      <input
                        id="modal-check-out"
                        type="date"
                        className="form-input"
                        value={checkOutDate}
                        min={minCheckOutDate}
                        onChange={(e) => setCheckOutDate(e.target.value)}
                        disabled={isSubmittingBooking}
                        required
                      />
                    </div>
                  </div>

                  {/* Dynamic Pricing Summary */}
                  <div className="modal-calculation-box">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
                      <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                        Stay Duration:
                      </span>
                      <strong style={{ fontSize: '0.95rem' }}>
                        {nightsCount > 0 ? `${nightsCount} Night(s)` : 'Select valid dates'}
                      </strong>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-main)' }}>
                        Estimated Total:
                      </span>
                      <span style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--primary)' }}>
                        {formatINR(estimatedTotal)}
                      </span>
                    </div>

                    <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '0.6rem' }}>
                      ℹ️ <em>Final price is verified and calculated server-side by Booking Service upon confirmation.</em>
                    </p>
                  </div>

                  <div className="modal-actions-row">
                    <button
                      type="button"
                      className="btn-modal-cancel"
                      onClick={handleCloseModal}
                      disabled={isSubmittingBooking}
                    >
                      Cancel
                    </button>

                    <button
                      type="submit"
                      className="btn-modal-confirm"
                      disabled={isSubmittingBooking || nightsCount <= 0}
                    >
                      {isSubmittingBooking ? (
                        <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                          <span className="btn-spinner"></span> Creating Booking...
                        </span>
                      ) : (
                        `Confirm Booking • ${formatINR(estimatedTotal)}`
                      )}
                    </button>
                  </div>
                </form>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
};

export default HotelDetailsPage;
