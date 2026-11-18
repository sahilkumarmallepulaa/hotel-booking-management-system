import React from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
  const { currentUser, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header className="navbar">
      <div className="nav-content">
        <Link to="/" className="brand-logo">
          <div className="brand-icon">🏨</div>
          <span>GrandStay</span>
        </Link>

        <nav>
          <ul className="nav-links">
            <li>
              <NavLink
                to="/"
                className={({ isActive }) =>
                  `nav-link ${isActive ? 'active' : ''}`
                }
              >
                Home
              </NavLink>
            </li>
            <li>
              <NavLink
                to="/hotels"
                className={({ isActive }) =>
                  `nav-link ${isActive ? 'active' : ''}`
                }
              >
                Hotels
              </NavLink>
            </li>
            {isAuthenticated && (
              <li>
                <NavLink
                  to="/my-bookings"
                  className={({ isActive }) =>
                    `nav-link ${isActive ? 'active' : ''}`
                  }
                >
                  My Bookings
                </NavLink>
              </li>
            )}
          </ul>
        </nav>

        <div className="nav-actions">
          {isAuthenticated ? (
            <div className="nav-user-area">
              <span className="user-greeting-chip" title={currentUser?.email}>
                <span className="user-avatar-dot">👤</span>
                <span className="user-greeting-name">
                  {currentUser?.name || currentUser?.email || 'User'}
                </span>
              </span>
              <button
                type="button"
                className="btn-signout"
                onClick={handleLogout}
              >
                Sign Out
              </button>
            </div>
          ) : (
            <>
              <Link to="/login" className="btn-signin">
                Sign In
              </Link>
              <Link to="/register" className="btn-register">
                Register
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
};

export default Navbar;
