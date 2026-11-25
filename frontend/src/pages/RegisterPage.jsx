import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { userClient } from '../services/api';

const RegisterPage = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  // If already authenticated, redirect to home
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  // Frontend validation aligned with User Service RegisterRequestDTO contract
  const validate = () => {
    const errors = {};

    if (!name.trim()) {
      errors.name = 'Full name is required.';
    }

    if (!email.trim()) {
      errors.email = 'Email address is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = 'Please enter a valid email address.';
    }

    if (!password) {
      errors.password = 'Password is required.';
    } else if (password.length < 6) {
      errors.password = 'Password must be at least 6 characters long.';
    }

    if (!confirmPassword) {
      errors.confirmPassword = 'Please confirm your password.';
    } else if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
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
      // POST /api/auth/register contract: { name, email, password }
      // Do NOT send confirmPassword to backend
      const normalizedEmail = email.trim().toLowerCase();
      await userClient.post('/api/auth/register', {
        name: name.trim(),
        email: normalizedEmail,
        password,
      });

      // After successful registration, redirect to login page with success notification
      // Do not automatically authenticate (backend register returns UserResponseDTO, not JWT)
      navigate('/login', {
        state: {
          registrationSuccess: true,
          registeredEmail: normalizedEmail,
        },
      });
    } catch (err) {
      console.error('Registration failed:', err);
      if (err.response) {
        if (err.response.status === 409) {
          setErrorMessage(
            err.response.data?.message ||
              'This email address is already registered. Please sign in or use a different email.'
          );
        } else if (err.response.status === 400) {
          const valErrors = err.response.data?.validationErrors;
          if (valErrors && typeof valErrors === 'object') {
            setFieldErrors(valErrors);
            setErrorMessage('Please correct the highlighted form errors.');
          } else {
            setErrorMessage(
              err.response.data?.message ||
                'Invalid registration details. Please verify your input.'
            );
          }
        } else {
          setErrorMessage(
            err.response.data?.message ||
              `Registration failed with status ${err.response.status}. Please try again.`
          );
        }
      } else {
        setErrorMessage(
          'Unable to connect to User Service (port 8083). Please verify the service is running.'
        );
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card" style={{ maxWidth: '480px' }}>
        <div className="login-icon">🏨</div>
        <h1 className="login-title">Join GrandStay</h1>
        <p className="login-subtitle">
          Create an account to unlock instant luxury room reservations and manage your stays.
        </p>

        {errorMessage && (
          <div className="login-error-banner" role="alert">
            <span>⚠️</span>
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate className="login-form">
          {/* Full Name */}
          <div className="form-group">
            <label htmlFor="register-name" className="form-label">
              Full Name
            </label>
            <input
              id="register-name"
              type="text"
              className={`form-input ${fieldErrors.name ? 'input-error' : ''}`}
              placeholder="e.g. Eleanor Vance"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (fieldErrors.name) setFieldErrors((prev) => ({ ...prev, name: null }));
              }}
              disabled={isSubmitting}
              autoComplete="name"
              required
            />
            {fieldErrors.name && (
              <span className="field-error-text">{fieldErrors.name}</span>
            )}
          </div>

          {/* Email Address */}
          <div className="form-group">
            <label htmlFor="register-email" className="form-label">
              Email Address
            </label>
            <input
              id="register-email"
              type="email"
              className={`form-input ${fieldErrors.email ? 'input-error' : ''}`}
              placeholder="e.g. eleanor@example.com"
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

          {/* Password */}
          <div className="form-group">
            <label htmlFor="register-password" className="form-label">
              Password
            </label>
            <input
              id="register-password"
              type="password"
              className={`form-input ${fieldErrors.password ? 'input-error' : ''}`}
              placeholder="Minimum 6 characters"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (fieldErrors.password) setFieldErrors((prev) => ({ ...prev, password: null }));
              }}
              disabled={isSubmitting}
              autoComplete="new-password"
              required
            />
            {fieldErrors.password && (
              <span className="field-error-text">{fieldErrors.password}</span>
            )}
          </div>

          {/* Confirm Password */}
          <div className="form-group">
            <label htmlFor="register-confirm-password" className="form-label">
              Confirm Password
            </label>
            <input
              id="register-confirm-password"
              type="password"
              className={`form-input ${fieldErrors.confirmPassword ? 'input-error' : ''}`}
              placeholder="Re-enter your password"
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                if (fieldErrors.confirmPassword) setFieldErrors((prev) => ({ ...prev, confirmPassword: null }));
              }}
              disabled={isSubmitting}
              autoComplete="new-password"
              required
            />
            {fieldErrors.confirmPassword && (
              <span className="field-error-text">{fieldErrors.confirmPassword}</span>
            )}
          </div>

          <button
            type="submit"
            className="btn-login-submit"
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                <span className="btn-spinner"></span> Creating Account...
              </span>
            ) : (
              'Create Account →'
            )}
          </button>
        </form>

        <div className="login-footer">
          <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)' }}>
            Already have an account?{' '}
            <Link to="/login" style={{ color: 'var(--primary)', fontWeight: 600 }}>
              Sign in here
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

export default RegisterPage;
