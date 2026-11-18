import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import bookingService from '../services/bookingService';
import hotelService from '../services/hotelService';
import { formatINR } from '../utils/formatters';

const calculateNights = (inDate, outDate) => {
  if (!inDate || !outDate) return 0;
  const d1 = new Date(inDate);
  const d2 = new Date(outDate);
  const diffTime = d2 - d1;
  return Math.max(1, Math.round(diffTime / (1000 * 60 * 60 * 24)));
};

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return dateStr;
  }
};

const formatDateTime = (dateStr) => {
  if (!dateStr) return null;
  try {
    const d = new Date(dateStr);
    return d.toLocaleString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateStr;
  }
};

const MyBookingsPage = () => {
  const { currentUser } = useAuth();

  const [bookings, setBookings] = useState([]);
  const [hotelsMap, setHotelsMap] = useState(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Cancellation modal state
  const [cancellingBooking, setCancellingBooking] = useState(null);
  const [isCancelling, setIsCancelling] = useState(false);
  const [cancelError, setCancelError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Load bookings and hotel enrichment data
  const loadData = useCallback(async () => {
    if (!currentUser?.id) return;
    setLoading(true);
    setError(null);

    try {
      // Parallel fetch of user bookings and hotel catalog for enrichment
      const [bookingsData, hotelsData] = await Promise.all([
        bookingService.getBookingsByUserId(currentUser.id),
        hotelService.getAllHotels().catch((err) => {
          console.warn('Failed to fetch hotels for booking enrichment:', err);
          return [];
        }),
      ]);

      // Normalize bookings response to array
      const rawBookings = Array.isArray(bookingsData)
        ? bookingsData
        : bookingsData
        ? [bookingsData]
        : [];

      // Sort bookings: most recent first (by createdAt or id)
      rawBookings.sort((a, b) => (b.id || 0) - (a.id || 0));
      setBookings(rawBookings);

      // Build quick lookup map for hotels and rooms
      const map = new Map();
      if (Array.isArray(hotelsData)) {
        hotelsData.forEach((hotel) => {
          const roomMap = new Map();
          if (Array.isArray(hotel.rooms)) {
            hotel.rooms.forEach((room) => {
              roomMap.set(room.id, room);
            });
          }
          map.set(hotel.id, {
            ...hotel,
            roomMap,
          });
        });
      }
      setHotelsMap(map);
    } catch (err) {
      console.error('Failed to load user bookings:', err);
      const apiMessage =
        err.response?.data?.message ||
        err.message ||
        'Failed to connect to Booking Service (port 8082).';
      setError(apiMessage);
    } finally {
      setLoading(false);
    }
  }, [currentUser?.id]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Handle keyboard Escape to dismiss cancellation dialog
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && cancellingBooking && !isCancelling) {
        setCancellingBooking(null);
        setCancelError(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [cancellingBooking, isCancelling]);

  // Open cancellation confirmation dialog
  const handleOpenCancelDialog = (booking) => {
    if (booking.bookingStatus === 'CANCELLED') return;
    setCancelError(null);
    setCancellingBooking(booking);
  };

  // Perform cancellation via API
  const handleConfirmCancellation = async () => {
    if (!cancellingBooking || isCancelling) return;
    setIsCancelling(true);
    setCancelError(null);

    try {
      const response = await bookingService.cancelBooking(cancellingBooking.id);

      // Update displayed status only after confirmed backend response
      setBookings((prev) =>
        prev.map((b) =>
          b.id === cancellingBooking.id
            ? { ...b, bookingStatus: response.bookingStatus || 'CANCELLED' }
            : b
        )
      );

      setSuccessMessage(
        `Booking #${cancellingBooking.id} has been successfully cancelled.`
      );
      setCancellingBooking(null);
    } catch (err) {
      console.error('Cancellation failed:', err);
      const apiMessage =
        err.response?.data?.message ||
        err.message ||
        'Unable to cancel booking. Please try again.';
      setCancelError(apiMessage);
    } finally {
      setIsCancelling(false);
    }
  };

  // Resolve enriched hotel and room details
  const getBookingDetails = useCallback(
    (booking) => {
      const hotel = hotelsMap.get(booking.hotelId);
      const room = hotel?.roomMap?.get(booking.roomId);

      return {
        hotelName: hotel?.name || `Hotel #${booking.hotelId}`,
        hotelLocation: hotel?.location || 'Location details unavailable',
        hotelRating: hotel?.rating,
        roomType: room?.roomType ? `${room.roomType} Room` : `Room ID #${booking.roomId}`,
        roomNumber: room?.roomNumber ? `Room #${room.roomNumber}` : null,
      };
    },
    [hotelsMap]
  );

  return (
    <div className="bookings-page">
      {/* Breadcrumb Navigation */}
      <nav className="breadcrumb-nav" aria-label="Breadcrumb">
        <Link to="/" className="breadcrumb-link">
          Home
        </Link>
        <span className="breadcrumb-sep">/</span>
        <span className="breadcrumb-current">My Bookings</span>
      </nav>

      <div className="bookings-page-header">
        <div>
          <h1 className="bookings-page-title">My Bookings</h1>
          <p className="bookings-page-subtitle">
            Manage and view all your room reservations for <strong>{currentUser?.name || currentUser?.email}</strong>.
          </p>
        </div>
        <Link to="/hotels" className="btn-browse-more">
          + Explore Hotels
        </Link>
      </div>

      {/* Success Notification Banner */}
      {successMessage && (
        <div className="bookings-success-banner" role="status">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <span>✓</span>
            <span>{successMessage}</span>
          </div>
          <button
            type="button"
            className="btn-dismiss-banner"
            onClick={() => setSuccessMessage(null)}
            aria-label="Dismiss message"
          >
            ✕
          </button>
        </div>
      )}

      {/* Loading State */}
      {loading && (
        <div className="state-container" role="status" aria-live="polite">
          <div className="loading-box">
            <progress aria-label="Loading your bookings" className="loading-spinner"></progress>
            <div className="state-title">Retrieving Your Bookings</div>
            <div className="state-desc">
              Loading your reservations and syncing with Hotel Service...
            </div>
          </div>
        </div>
      )}

      {/* Error State */}
      {!loading && error && (
        <div className="state-container state-error" role="alert">
          <div className="state-icon">⚠️</div>
          <h2 className="state-title">Unable to Load Bookings</h2>
          <p className="state-desc">
            An error occurred while fetching your bookings:
            <br />
            <strong>{error}</strong>
          </p>
          <button
            type="button"
            className="btn-state-action btn-state-retry"
            onClick={loadData}
          >
            🔄 Try Again
          </button>
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && bookings.length === 0 && (
        <div className="state-container state-empty" role="status">
          <div className="state-icon">🧳</div>
          <h2 className="state-title">No Bookings Yet</h2>
          <p className="state-desc">
            You don't have any active or past reservations. Discover our distinguished luxury properties and book your first stay with GrandStay!
          </p>
          <Link to="/hotels" className="btn-state-action">
            Browse Luxury Hotels &rarr;
          </Link>
        </div>
      )}

      {/* Bookings List */}
      {!loading && !error && bookings.length > 0 && (
        <div className="bookings-list-container">
          <div className="bookings-count-row">
            <span>
              Showing <strong>{bookings.length}</strong> Reservation{bookings.length === 1 ? '' : 's'}
            </span>
          </div>

          <div className="bookings-cards-grid">
            {bookings.map((booking) => {
              const details = getBookingDetails(booking);
              const nights = calculateNights(booking.checkInDate, booking.checkOutDate);
              const isConfirmed = booking.bookingStatus === 'CONFIRMED';
              const isCancelled = booking.bookingStatus === 'CANCELLED';

              return (
                <article
                  key={booking.id}
                  className={`booking-card ${isCancelled ? 'booking-card-cancelled' : ''}`}
                >
                  {/* Card Header */}
                  <div className="booking-card-header">
                    <div className="booking-card-ref">
                      <span className="booking-ref-tag">Reference</span>
                      <strong className="booking-ref-number">#{booking.id}</strong>
                    </div>

                    <div className="booking-status-wrapper">
                      {isConfirmed && (
                        <span className="status-badge status-badge-confirmed">
                          ● Confirmed
                        </span>
                      )}
                      {isCancelled && (
                        <span className="status-badge status-badge-cancelled">
                          ✕ Cancelled
                        </span>
                      )}
                      {!isConfirmed && !isCancelled && (
                        <span className="status-badge status-badge-other">
                          {booking.bookingStatus}
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Property & Room Info */}
                  <div className="booking-property-info">
                    <div className="booking-hotel-name-row">
                      <h2 className="booking-hotel-name">
                        <Link to={`/hotels/${booking.hotelId}`} className="hotel-link">
                          {details.hotelName}
                        </Link>
                      </h2>
                      {typeof details.hotelRating === 'number' && (
                        <span className="booking-hotel-rating">
                          ★ {details.hotelRating.toFixed(1)}
                        </span>
                      )}
                    </div>

                    <div className="booking-location-text">
                      📍 {details.hotelLocation}
                    </div>

                    <div className="booking-room-pill">
                      <span>🛏️ {details.roomType}</span>
                      {details.roomNumber && (
                        <>
                          <span>&bull;</span>
                          <span>{details.roomNumber}</span>
                        </>
                      )}
                      <span>&bull;</span>
                      <span style={{ color: 'var(--text-muted)' }}>Room ID: {booking.roomId}</span>
                    </div>
                  </div>

                  {/* Dates & Nights Breakdown */}
                  <div className="booking-dates-grid">
                    <div className="booking-date-block">
                      <span className="booking-date-label">Check-In</span>
                      <strong className="booking-date-value">
                        {formatDate(booking.checkInDate)}
                      </strong>
                    </div>

                    <div className="booking-duration-block">
                      <span className="booking-nights-badge">
                        {nights} {nights === 1 ? 'Night' : 'Nights'}
                      </span>
                      <div className="booking-arrow-line">&rarr;</div>
                    </div>

                    <div className="booking-date-block" style={{ textAlign: 'right' }}>
                      <span className="booking-date-label">Check-Out</span>
                      <strong className="booking-date-value">
                        {formatDate(booking.checkOutDate)}
                      </strong>
                    </div>
                  </div>

                  {/* Card Footer: Pricing & Actions */}
                  <div className="booking-card-footer">
                    <div className="booking-price-box">
                      <span className="booking-price-label">Total Amount</span>
                      <strong className="booking-price-amount">
                        {formatINR(booking.totalPrice)}
                      </strong>
                      <span className="booking-price-tax-note">incl. taxes & fees</span>
                    </div>

                    <div className="booking-action-box">
                      {isConfirmed && (
                        <button
                          type="button"
                          className="btn-cancel-booking"
                          onClick={() => handleOpenCancelDialog(booking)}
                        >
                          Cancel Booking
                        </button>
                      )}

                      {isCancelled && (
                        <span className="booking-cancelled-label">
                          Reservation Cancelled
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Timestamp if present */}
                  {booking.createdAt && (
                    <div className="booking-card-timestamp">
                      Booked on {formatDateTime(booking.createdAt)}
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        </div>
      )}

      {/* Cancellation Confirmation Dialog */}
      {cancellingBooking && (
        <div
          className="modal-overlay"
          onClick={() => {
            if (!isCancelling) {
              setCancellingBooking(null);
              setCancelError(null);
            }
          }}
        >
          <div
            className="modal-card cancel-dialog-card"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="cancel-dialog-title"
          >
            <div className="modal-header">
              <span className="modal-icon" style={{ color: '#dc2626' }}>
                ⚠️
              </span>
              <div>
                <h2 id="cancel-dialog-title" className="modal-title">
                  Cancel Reservation?
                </h2>
                <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>
                  Booking Reference #{cancellingBooking.id}
                </p>
              </div>
            </div>

            <p className="cancel-dialog-desc">
              Are you sure you want to cancel your stay at{' '}
              <strong>{getBookingDetails(cancellingBooking).hotelName}</strong>?
            </p>

            <div className="cancel-summary-box">
              <div className="cancel-summary-row">
                <span>Dates:</span>
                <strong>
                  {formatDate(cancellingBooking.checkInDate)} &ndash;{' '}
                  {formatDate(cancellingBooking.checkOutDate)}
                </strong>
              </div>
              <div className="cancel-summary-row">
                <span>Total Amount:</span>
                <strong style={{ color: 'var(--primary)' }}>
                  {formatINR(cancellingBooking.totalPrice)}
                </strong>
              </div>
              <div className="cancel-summary-row">
                <span>Cancellation Policy:</span>
                <strong style={{ color: '#059669' }}>Free Cancellation</strong>
              </div>
            </div>

            {/* Error in modal */}
            {cancelError && (
              <div className="modal-error-banner" role="alert">
                <span>⚠️</span>
                <span>{cancelError}</span>
              </div>
            )}

            <div className="modal-actions-row">
              <button
                type="button"
                className="btn-modal-cancel"
                onClick={() => {
                  setCancellingBooking(null);
                  setCancelError(null);
                }}
                disabled={isCancelling}
              >
                No, Keep Booking
              </button>

              <button
                type="button"
                className="btn-danger-confirm"
                onClick={handleConfirmCancellation}
                disabled={isCancelling}
              >
                {isCancelling ? (
                  <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                    <span className="btn-spinner"></span> Cancelling...
                  </span>
                ) : (
                  'Yes, Cancel Booking'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default MyBookingsPage;
