import React, { createContext, useState, useContext, useEffect } from 'react';
import { login as apiLogin } from '../services/api';
import { jwtDecode } from 'jwt-decode'; // You need to install this: npm install jwt-decode

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(localStorage.getItem('token'));
    const [user, setUser] = useState(null);

    useEffect(() => {
        if (token) {
            try {
                const decoded = jwtDecode(token);
                setUser({ username: decoded.sub });
                localStorage.setItem('token', token);
            } catch (error) {
                console.error("Invalid token:", error);
                logout();
            }
        } else {
            localStorage.removeItem('token');
            setUser(null);
        }
    }, [token]);

    const login = async (username, password) => {
        const newToken = await apiLogin(username, password);
        setToken(newToken);
    };

    const logout = () => {
        setToken(null);
    };

    const value = { token, user, login, logout };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    return useContext(AuthContext);
};