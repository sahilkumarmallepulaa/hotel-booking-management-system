import React, { createContext, useContext, useState, useEffect } from 'react';
import { userClient } from '../services/api';

const AuthContext = createContext(null);

const STORAGE_TOKEN_KEY = 'grandstay_token';
const STORAGE_USER_KEY = 'grandstay_user';

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_TOKEN_KEY) || null;
    } catch {
      return null;
    }
  });

  const [currentUser, setCurrentUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem(STORAGE_USER_KEY);
      return savedUser ? JSON.parse(savedUser) : null;
    } catch {
      return null;
    }
  });

  // Login handler using live User Service POST /api/auth/login contract
  const login = async (email, password) => {
    const response = await userClient.post('/api/auth/login', { email, password });
    const { token: jwtToken, user } = response.data;

    // Never store passwords in localStorage
    if (jwtToken && user) {
      try {
        localStorage.setItem(STORAGE_TOKEN_KEY, jwtToken);
        localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(user));
      } catch (err) {
        console.warn('Failed to persist session to localStorage:', err);
      }
      setToken(jwtToken);
      setCurrentUser(user);
    }

    return response.data;
  };

  // Logout handler
  const logout = () => {
    try {
      localStorage.removeItem(STORAGE_TOKEN_KEY);
      localStorage.removeItem(STORAGE_USER_KEY);
    } catch (err) {
      console.warn('Failed to clear localStorage on logout:', err);
    }
    setToken(null);
    setCurrentUser(null);
  };

  const value = {
    currentUser,
    token,
    isAuthenticated: !!token,
    login,
    logout,
    setCurrentUser,
    setToken,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
