import React, { useState } from 'react';

const HeroSection = ({ onSearch }) => {
  const [destination, setDestination] = useState('Mumbai');
  const [checkInDate, setCheckInDate] = useState('2026-12-01');
  const [checkOutDate, setCheckOutDate] = useState('2026-12-05');
  const [guests, setGuests] = useState('2');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (onSearch) {
      onSearch({ destination, checkInDate, checkOutDate, guests });
    }
  };

  return (
    <section className="hero-section">
      <div className="hero-overlay"></div>
      <div className="hero-content">
        <div className="hero-badge">
          <span>✨ Discover Luxury Escapes</span>
        </div>
        <h1 className="hero-title">Find & Book Your Perfect Hotel Stay</h1>
        <p className="hero-subtitle">
          Experience premium comfort, exclusive rates, and flexible bookings at
          world-class luxury hotels and resorts.
        </p>
      </div>

      <div className="search-widget-container">
        <form className="search-widget" onSubmit={handleSubmit}>
          <div className="search-field">
            <label className="field-label" htmlFor="destination">
              Destination / City
            </label>
            <input
              id="destination"
              type="text"
              className="field-input"
              placeholder="e.g. Mumbai, Goa, Delhi"
              value={destination}
              onChange={(e) => setDestination(e.target.value)}
              required
            />
          </div>

          <div className="search-field">
            <label className="field-label" htmlFor="checkIn">
              Check-in Date
            </label>
            <input
              id="checkIn"
              type="date"
              className="field-input"
              value={checkInDate}
              onChange={(e) => setCheckInDate(e.target.value)}
              required
            />
          </div>

          <div className="search-field">
            <label className="field-label" htmlFor="checkOut">
              Check-out Date
            </label>
            <input
              id="checkOut"
              type="date"
              className="field-input"
              value={checkOutDate}
              onChange={(e) => setCheckOutDate(e.target.value)}
              required
            />
          </div>

          <div className="search-field">
            <label className="field-label" htmlFor="guests">
              Guests
            </label>
            <select
              id="guests"
              className="field-input"
              value={guests}
              onChange={(e) => setGuests(e.target.value)}
            >
              <option value="1">1 Guest</option>
              <option value="2">2 Guests</option>
              <option value="3">3 Guests</option>
              <option value="4">4+ Guests</option>
            </select>
          </div>

          <button type="submit" className="btn-search">
            <span>🔍</span>
            <span>Search</span>
          </button>
        </form>
      </div>
    </section>
  );
};

export default HeroSection;
