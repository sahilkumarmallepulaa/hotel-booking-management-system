import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import hotelService from '../services/hotelService';
import { formatINR } from '../utils/formatters';

// High quality curated hotel photos for display cards
const HOTEL_IMAGES = [
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=800&q=80',
];

const FeaturedHotels = () => {
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchHotels = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await hotelService.getAllHotels();
      if (Array.isArray(data)) {
        setHotels(data);
      } else {
        setHotels([]);
      }
    } catch (err) {
      console.error('Failed to fetch hotels from Hotel Service:', err);
      const message =
        err.response?.data?.message ||
        err.message ||
        'Unable to communicate with Hotel Service. Please verify the service is running on port 8081.';
      setError(message);
      setHotels([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchHotels();
  }, [fetchHotels]);

  return (
    <section className="section" id="featured-hotels">
      <div className="section-header">
        <div>
          <div className="section-pretitle">Handpicked Accommodations</div>
          <h2 className="section-title">Featured Luxury Stays</h2>
          <p className="section-description">
            Explore our destinations with real-time room availability and verified pricing directly from Hotel Service.
          </p>
        </div>

        <div>
          {loading && (
            <span className="notice-badge notice-fallback">
              ⏳ Connecting to Hotel Service...
            </span>
          )}
          {!loading && error && (
            <span className="notice-badge notice-error">
              🔴 Hotel Service Offline
            </span>
          )}
          {!loading && !error && hotels.length === 0 && (
            <span className="notice-badge notice-empty">
              ⚪ 0 Hotels Listed
            </span>
          )}
          {!loading && !error && hotels.length > 0 && (
            <span className="notice-badge notice-live">
              🟢 Live Hotel Service ({hotels.length} Hotel{hotels.length > 1 ? 's' : ''})
            </span>
          )}
        </div>
      </div>

      {/* 1. Loading State */}
      {loading && (
        <div className="state-container" role="status" aria-live="polite">
          <div className="loading-box">
            <progress aria-label="Loading hotels" className="loading-spinner"></progress>
            <div className="state-title">Loading Luxury Hotels</div>
            <div className="state-desc">
              Retrieving live hotel inventory and room rates from Hotel Service (port 8081)...
            </div>
          </div>
        </div>
      )}

      {/* 2. Error State */}
      {!loading && error && (
        <div className="state-container state-error" role="alert">
          <div className="state-icon">⚠️</div>
          <h3 className="state-title">Unable to Load Hotels</h3>
          <p className="state-desc">
            Failed to connect to the backend Hotel Service API (<code>GET /api/hotels</code>):
            <br />
            <strong>{error}</strong>
          </p>
          <button
            type="button"
            className="btn-state-action btn-state-retry"
            onClick={fetchHotels}
          >
            🔄 Retry Connection
          </button>
        </div>
      )}

      {/* 3. Empty State */}
      {!loading && !error && hotels.length === 0 && (
        <div className="state-container state-empty">
          <div className="state-icon">🏨</div>
          <h3 className="state-title">No Hotels Found</h3>
          <p className="state-desc">
            No hotels are currently registered in the database. Add a new hotel via the Hotel Service API to view it here.
          </p>
          <button
            type="button"
            className="btn-state-action"
            onClick={fetchHotels}
          >
            🔄 Refresh Hotels
          </button>
        </div>
      )}

      {/* 4. Populated Success State */}
      {!loading && !error && hotels.length > 0 && (
        <div className="hotel-grid">
          {hotels.map((hotel, index) => {
            const hasRooms = Array.isArray(hotel.rooms) && hotel.rooms.length > 0;
            const primaryRoom = hasRooms ? hotel.rooms[0] : null;
            const price = primaryRoom ? primaryRoom.pricePerNight : null;
            const roomType = primaryRoom ? primaryRoom.roomType : null;
            const capacity = primaryRoom ? primaryRoom.capacity : null;
            const imageUrl = HOTEL_IMAGES[index % HOTEL_IMAGES.length];

            return (
              <article key={hotel.id || index} className="hotel-card">
                <div className="card-image-wrap">
                  <Link to={`/hotels/${hotel.id}`} title={`View details for ${hotel.name}`}>
                    <img
                      src={imageUrl}
                      alt={hotel.name}
                      className="card-image"
                      loading="lazy"
                    />
                  </Link>
                  <div className="card-rating-badge">
                    <span>★</span>
                    <span>
                      {typeof hotel.rating === 'number'
                        ? hotel.rating.toFixed(1)
                        : '5.0'}
                    </span>
                  </div>
                </div>

                <div className="card-body">
                  <div className="card-location">
                    <span>📍</span>
                    <span>{hotel.location}</span>
                  </div>

                  <h3 className="card-title">
                    <Link to={`/hotels/${hotel.id}`} className="card-title-link">
                      {hotel.name}
                    </Link>
                  </h3>
                  <p className="card-desc">
                    {hotel.description ||
                      'Experience world-class luxury and unparalleled hospitality.'}
                  </p>

                  <div className="card-room-info">
                    {hasRooms ? (
                      <>
                        <span>🛏️ {roomType} Room</span>
                        <span>👥 Up to {capacity} Guests</span>
                      </>
                    ) : (
                      <>
                        <span>🛏️ Room details</span>
                        <span>Inquire at reception</span>
                      </>
                    )}
                  </div>

                  <div className="card-footer">
                    <div className="price-box">
                      {price !== null ? (
                        <>
                          <span className="price-amount">{formatINR(price)}</span>
                          <span className="price-period">per night + taxes</span>
                        </>
                      ) : (
                        <>
                          <span className="price-amount">Contact</span>
                          <span className="price-period">for custom quote</span>
                        </>
                      )}
                    </div>

                    <Link
                      to={`/hotels/${hotel.id}`}
                      className="btn-card-action"
                    >
                      View Details &rarr;
                    </Link>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
};

export default FeaturedHotels;
