const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const getToken = () => localStorage.getItem('pulse_jwt');
export const setToken = (token) => localStorage.setItem('pulse_jwt', token);
export const clearToken = () => localStorage.removeItem('pulse_jwt');

// Background pre-warm to wake up sleeping cloud instances (Render/Neon)
export const warmupBackend = () => {
    try {
        fetch(`${BASE_URL}/api/auth/health`, { method: 'GET', mode: 'cors' }).catch(() => {});
    } catch (e) {}
};

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

    // 60-second timeout controller for cloud cold starts
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), options.timeout || 65000);

    try {
        const response = await fetch(url, {
            ...options,
            headers,
            signal: options.signal || controller.signal,
        });

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
        if (error.name === 'AbortError') {
            throw new Error('Connection timed out. Cloud server took too long to respond. Please retry.');
        }
        throw error;
    } finally {
        clearTimeout(timeoutId);
    }
};

export const getResearchPresets = () => apiFetch('/api/research/presets');

export const streamResearch = ({ query, symbols = [], onStatus, onContext, onChunk, onComplete, onError }) => {
    const token = getToken();
    let url = `${BASE_URL}/api/research/stream?query=${encodeURIComponent(query)}`;
    if (token) {
        url += `&token=${encodeURIComponent(token)}`;
    }
    if (symbols && symbols.length > 0) {
        symbols.forEach(s => {
            url += `&symbols=${encodeURIComponent(s)}`;
        });
    }

    const eventSource = new EventSource(url);

    eventSource.addEventListener('status', (e) => {
        try {
            const data = JSON.parse(e.data);
            if (onStatus) onStatus(data.message || data);
        } catch (err) {
            console.error('Error parsing status event', err);
        }
    });

    eventSource.addEventListener('context', (e) => {
        try {
            const data = JSON.parse(e.data);
            if (onContext) onContext(data);
        } catch (err) {
            console.error('Error parsing context event', err);
        }
    });

    eventSource.addEventListener('chunk', (e) => {
        try {
            const data = JSON.parse(e.data);
            if (onChunk) onChunk(data.content || '');
        } catch (err) {
            console.error('Error parsing chunk event', err);
        }
    });

    eventSource.addEventListener('complete', (e) => {
        try {
            const data = JSON.parse(e.data);
            if (onComplete) onComplete(data);
        } catch (err) {
            console.error('Error parsing complete event', err);
        }
        eventSource.close();
    });

    eventSource.addEventListener('error', (err) => {
        if (onError) onError(err);
        eventSource.close();
    });

    return () => {
        eventSource.close();
    };
};

