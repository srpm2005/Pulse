import { useState } from 'react';
import { apiFetch, setToken } from '../services/api';
import '../login.css'; // Mapped from frontend-legacy

export default function Login({ onLoginComplete }) {
    const [isLogin, setIsLogin] = useState(true);
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        const endpoint = isLogin ? '/api/auth/login' : '/api/auth/register';
        const payload = { email, password };

        try {
            const data = await apiFetch(endpoint, {
                method: 'POST',
                body: JSON.stringify(payload),
            });

            if (data.token) {
                setToken(data.token);
                onLoginComplete();
            } else {
                setError('Authentication failed. No token received.');
            }
        } catch (err) {
            setError(err.message || 'An error occurred during authentication.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <div className="card-glow"></div>
                <div className="logo">
                    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M22 12h-4l-3 9L9 3l-3 9H2"></path>
                    </svg>
                    <span>Pulse</span>
                </div>

                <h1 id="form-title">{isLogin ? 'Welcome back' : 'Create an account'}</h1>
                <p className="subtitle" id="form-subtitle">
                    {isLogin ? 'Enter your details to access your dashboard.' : 'Enter your details to start tracking stocks.'}
                </p>

                {error && <div className="error-banner">{error}</div>}

                <form id="auth-form" onSubmit={handleSubmit}>
                    <div className="input-group">
                        <label htmlFor="email">Email</label>
                        <input
                            type="email"
                            id="email"
                            required
                            placeholder="name@example.com"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                        />
                    </div>
                    <div className="input-group">
                        <label htmlFor="password">Password</label>
                        <input
                            type="password"
                            id="password"
                            required
                            placeholder="••••••••"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                        />
                    </div>

                    <button type="submit" id="submit-btn" className="btn-primary" disabled={loading} style={{ width: '100%' }}>
                        {loading ? 'Processing...' : (isLogin ? 'Sign In' : 'Sign Up')}
                    </button>
                </form>

                <div className="toggle-mode">
                    <span id="toggle-text">{isLogin ? "Don't have an account? " : "Already have an account? "}</span>
                    <button type="button" id="toggle-btn" className="btn-link" onClick={() => setIsLogin(!isLogin)}>
                        {isLogin ? 'Sign Up' : 'Sign In'}
                    </button>
                </div>
            </div>
        </div>
    );
}
