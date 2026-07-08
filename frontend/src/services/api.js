const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const getToken = () => localStorage.getItem('pulse_jwt');
export const setToken = (token) => localStorage.setItem('pulse_jwt', token);
export const clearToken = () => localStorage.removeItem('pulse_jwt');

const authHeaders = () => {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
};

export const apiFetch = async (path, options = {}) => {
    const url = `${BASE_URL}${path}`;
    const headers = options.headers || authHeaders();

    try {
        const response = await fetch(url, { ...options, headers });

        if (response.status === 401 && path !== '/api/auth/login') {
            clearToken();
            window.location.reload();
            return null;
        }

        const text = await response.text();
        const data = text ? JSON.parse(text) : null;

        if (!response.ok) {
            const errorMsg = data?.message || data?.error || 'An unexpected error occurred';
            throw new Error(errorMsg);
        }

        return data;
    } catch (error) {
        throw error;
    }
};
