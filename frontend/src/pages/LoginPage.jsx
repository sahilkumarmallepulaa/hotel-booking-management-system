import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const LoginPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated } = useAuth();

  const [email, setEmail] = useState(() => location.state?.registeredEmail || '');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  // Where to navigate after successful login
  const returnUrl = location.state?.from?.pathname || '/';

  // If already authenticated, redirect
  useEffect(() => {
    if (isAuthenticated) {
      navigate(returnUrl, { replace: true });
    }
  }, [isAuthenticated, navigate, returnUrl]);

  const validate = () => {
    const errors = {};
    if (!email.trim()) {
      errors.email = 'Email address is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = 'Please enter a valid email address.';
    }

    if (!password) {
      errors.password = 'Password is required.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await login(email.trim(), password);
      // Navigation is triggered by the auth state change or directly:
      navigate(returnUrl, { replace: true });
    } catch (err) {
      console.error('Login error:', err);
      const apiMessage =
        err.response?.data?.message ||
        (err.response?.status === 401
          ? 'Invalid email or password. Please check your credentials.'
          : err.response?.status === 400
          ? 'Invalid request format.'
          : 'Unable to connect to User Service (port 8083). Please verify the service is running.');
      setErrorMessage(apiMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-icon">🔐</div>
        <h1 className="login-title">Welcome to GrandStay</h1>
        <p className="login-subtitle">
          Sign in to your account to reserve luxury rooms and manage your bookings.
        </p>

        {location.state?.registrationSuccess && (
          <div className="login-success-banner" role="status">
            <span>✓</span>
            <span>Registration successful! Please sign in with your credentials.</span>
          </div>
        )}

        {location.state?.from && (
          <div className="login-notice-banner" role="status">
            ℹ️ Please sign in to complete your room reservation.
          </div>
        )}

        {errorMessage && (
          <div className="login-error-banner" role="alert">
            <span>⚠️</span>
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate className="login-form">
          <div className="form-group">
            <label htmlFor="login-email" className="form-label">
              Email Address
            </label>
            <input
              id="login-email"
              type="email"
              className={`form-input ${fieldErrors.email ? 'input-error' : ''}`}
              placeholder="e.g. testuser@example.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (fieldErrors.email) setFieldErrors((prev) => ({ ...prev, email: null }));
              }}
              disabled={isSubmitting}
              autoComplete="email"
              required
            />
            {fieldErrors.email && (
              <span className="field-error-text">{fieldErrors.email}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="login-password" className="form-label">
              Password
            </label>
            <input
              id="login-password"
              type="password"
              className={`form-input ${fieldErrors.password ? 'input-error' : ''}`}
              placeholder="Enter your password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (fieldErrors.password) setFieldErrors((prev) => ({ ...prev, password: null }));
              }}
              disabled={isSubmitting}
              autoComplete="current-password"
              required
            />
            {fieldErrors.password && (
              <span className="field-error-text">{fieldErrors.password}</span>
            )}
          </div>

          <button
            type="submit"
            className="btn-login-submit"
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                <span className="btn-spinner"></span> Authenticating...
              </span>
            ) : (
              'Sign In &rarr;'
            )}
          </button>
        </form>

        <div className="login-footer">
          <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>
            Need an account?{' '}
            <Link to="/register" style={{ color: 'var(--primary)', fontWeight: 600 }}>
              Register here
            </Link>
          </p>
          <div style={{ marginTop: '1rem' }}>
            <Link to="/" style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              &larr; Back to Home
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
