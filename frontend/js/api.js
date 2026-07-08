const BASE_URL = 'http://localhost:8080';

const getToken = () => localStorage.getItem('pulse_jwt');
const setToken = (token) => localStorage.setItem('pulse_jwt', token);
const clearToken = () => localStorage.removeItem('pulse_jwt');

const authHeaders = () => {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
};

const showToast = (message, type = 'error') => {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<span style="font-weight: 500;">${type === 'error' ? 'Error' : 'Success'}:</span> ${message}`;
    
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.add('hide');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
};

const apiFetch = async (path, options = {}) => {
    const url = `${BASE_URL}${path}`;
    const headers = options.headers || authHeaders();
    
    try {
        const response = await fetch(url, { ...options, headers });
        
        if (response.status === 401 && path !== '/api/auth/login') {
            clearToken();
            window.location.href = 'index.html';
            return null;
        }
        
        // Handle empty responses
        const text = await response.text();
        const data = text ? JSON.parse(text) : null;
        
        if (!response.ok) {
            const errorMsg = data?.message || data?.error || 'An unexpected error occurred';
            throw new Error(errorMsg);
        }
        
        return data;
    } catch (error) {
        showToast(error.message, 'error');
        throw error;
    }
};
