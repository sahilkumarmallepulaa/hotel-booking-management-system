import React from 'react';
import { Link } from 'react-router-dom';

const Footer = () => {
  return (
    <footer className="footer">
      <div className="footer-content">
        <div className="footer-brand">
          <div className="footer-logo">
            <span>🏨</span>
            <span>GrandStay</span>
          </div>
          <p className="footer-desc">
            Your trusted gateway to unforgettable hotel experiences worldwide.
            Powered by enterprise Spring Boot microservices.
          </p>
        </div>

        <div>
          <h5 className="footer-column-title">Explore</h5>
          <ul className="footer-links">
            <li>
              <Link to="/" className="footer-link">
                Home
              </Link>
            </li>
            <li>
              <Link to="/hotels" className="footer-link">
                Hotels & Suites
              </Link>
            </li>
            <li>
              <a href="#featured-hotels" className="footer-link">
                Featured Stays
              </a>
            </li>
          </ul>
        </div>

        <div>
          <h5 className="footer-column-title">Account</h5>
          <ul className="footer-links">
            <li>
              <Link to="/login" className="footer-link">
                Sign In
              </Link>
            </li>
            <li>
              <Link to="/register" className="footer-link">
                Register
              </Link>
            </li>
          </ul>
        </div>

        <div>
          <h5 className="footer-column-title">Architecture</h5>
          <ul className="footer-links">
            <li>
              <span className="footer-link">Eureka Server (8761)</span>
            </li>
            <li>
              <span className="footer-link">Hotel Service (8081)</span>
            </li>
            <li>
              <span className="footer-link">Booking Service (8082)</span>
            </li>
            <li>
              <span className="footer-link">User Service (8083)</span>
            </li>
          </ul>
        </div>
      </div>

      <div className="footer-bottom">
        <div>&copy; {new Date().getFullYear()} GrandStay Hospitality Ltd. All rights reserved.</div>
        <div>Hotel Booking Management System &bull; React &bull; Vite</div>
      </div>
    </footer>
  );
};

export default Footer;
