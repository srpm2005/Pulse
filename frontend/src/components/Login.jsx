import { useState, useEffect } from 'react';
import { apiFetch, setToken, warmupBackend } from '../services/api';
import '../login.css';

export default function Login({ onLoginComplete }) {
    // Modes: 'login' | 'register' | 'forgot' | 'reset'
    const [mode, setMode] = useState('login');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [resetCode, setResetCode] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState(null);
    const [loading, setLoading] = useState(false);
    const [coldStartNotice, setColdStartNotice] = useState(false);

    // Warm up the backend server immediately when the login page opens
    useEffect(() => {
        warmupBackend();
    }, []);

    // Timer to notify the user if a cloud cold-start is in progress
    useEffect(() => {
        let timer;
        if (loading) {
            timer = setTimeout(() => {
                setColdStartNotice(true);
            }, 3500);
        } else {
            setColdStartNotice(false);
        }
        return () => clearTimeout(timer);
    }, [loading]);

    const switchMode = (newMode) => {
        setMode(newMode);
        setError(null);
        setSuccessMessage(null);
        setPassword('');
        setResetCode('');
        setNewPassword('');
        setConfirmPassword('');
    };

    const handleLoginOrRegister = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccessMessage(null);
        setLoading(true);

        const endpoint = mode === 'login' ? '/api/auth/login' : '/api/auth/register';
        const payload = { email, password };

        try {
            const data = await apiFetch(endpoint, {
                method: 'POST',
                body: JSON.stringify(payload),
            });

            if (data?.token) {
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

    const handleForgotPassword = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccessMessage(null);
        setLoading(true);

        try {
            const data = await apiFetch('/api/auth/forgot-password', {
                method: 'POST',
                body: JSON.stringify({ email }),
            });

            setSuccessMessage(data?.message || 'Verification code sent to your email.');
            setMode('reset');
        } catch (err) {
            setError(err.message || 'Failed to send reset code. Please check your email.');
        } finally {
            setLoading(false);
        }
    };

    const handleResetPassword = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccessMessage(null);

        if (newPassword !== confirmPassword) {
            setError('New passwords do not match.');
            return;
        }

        if (newPassword.length < 6) {
            setError('Password must be at least 6 characters.');
            return;
        }

        setLoading(true);

        try {
            const data = await apiFetch('/api/auth/reset-password', {
                method: 'POST',
                body: JSON.stringify({
                    email,
                    code: resetCode.trim(),
                    newPassword,
                }),
            });

            setSuccessMessage(data?.message || 'Password successfully reset! Please sign in.');
            setMode('login');
            setPassword('');
        } catch (err) {
            setError(err.message || 'Failed to reset password. Code may be invalid or expired.');
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

                {/* Form Titles */}
                {mode === 'login' && (
                    <>
                        <h1 id="form-title">Welcome back</h1>
                        <p className="subtitle" id="form-subtitle">Enter your details to access your dashboard.</p>
                    </>
                )}
                {mode === 'register' && (
                    <>
                        <h1 id="form-title">Create an account</h1>
                        <p className="subtitle" id="form-subtitle">Enter your details to start tracking stocks.</p>
                    </>
                )}
                {mode === 'forgot' && (
                    <>
                        <h1 id="form-title">Forgot Password</h1>
                        <p className="subtitle" id="form-subtitle">Enter your registered email to receive a 6-digit reset code.</p>
                    </>
                )}
                {mode === 'reset' && (
                    <>
                        <h1 id="form-title">Set New Password</h1>
                        <p className="subtitle" id="form-subtitle">Enter the 6-digit code sent to your email and your new password.</p>
                    </>
                )}

                {/* Feedback Banners */}
                {error && <div className="error-banner">{error}</div>}
                {successMessage && <div className="success-banner">{successMessage}</div>}

                {/* Mode 1 & 2: Login / Register Form */}
                {(mode === 'login' || mode === 'register') && (
                    <form id="auth-form" onSubmit={handleLoginOrRegister}>
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
                            <div className="label-row">
                                <label htmlFor="password">Password</label>
                                {mode === 'login' && (
                                    <button
                                        type="button"
                                        className="btn-link forgot-link"
                                        onClick={() => switchMode('forgot')}
                                    >
                                        Forgot password?
                                    </button>
                                )}
                            </div>
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
                            {loading ? 'Processing...' : (mode === 'login' ? 'Sign In' : 'Sign Up')}
                        </button>

                        {coldStartNotice && (
                            <div className="cold-start-notice">
                                <span>⚡ Waking up server from idle sleep, please hold on...</span>
                            </div>
                        )}

                        <div className="toggle-mode">
                            <span>{mode === 'login' ? "Don't have an account? " : "Already have an account? "}</span>
                            <button
                                type="button"
                                id="toggle-btn"
                                className="btn-link"
                                onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}
                            >
                                {mode === 'login' ? 'Sign Up' : 'Sign In'}
                            </button>
                        </div>
                    </form>
                )}

                {/* Mode 3: Forgot Password Form */}
                {mode === 'forgot' && (
                    <form id="forgot-form" onSubmit={handleForgotPassword}>
                        <div className="input-group">
                            <label htmlFor="forgot-email">Registered Email</label>
                            <input
                                type="email"
                                id="forgot-email"
                                required
                                placeholder="name@example.com"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                            />
                        </div>

                        <button type="submit" className="btn-primary" disabled={loading} style={{ width: '100%' }}>
                            {loading ? 'Sending Code...' : 'Send Reset Code'}
                        </button>

                        {coldStartNotice && (
                            <div className="cold-start-notice">
                                <span>⚡ Connecting to cloud server, please hold on...</span>
                            </div>
                        )}

                        <div className="toggle-mode">
                            <button type="button" className="btn-link" onClick={() => switchMode('login')}>
                                ← Back to Sign In
                            </button>
                        </div>
                    </form>
                )}

                {/* Mode 4: Reset Password Form */}
                {mode === 'reset' && (
                    <form id="reset-form" onSubmit={handleResetPassword}>
                        <div className="input-group">
                            <label htmlFor="reset-email">Email</label>
                            <input
                                type="email"
                                id="reset-email"
                                required
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                            />
                        </div>

                        <div className="input-group">
                            <label htmlFor="reset-code">6-Digit Verification Code</label>
                            <input
                                type="text"
                                id="reset-code"
                                required
                                maxLength="6"
                                placeholder="123456"
                                className="code-input"
                                value={resetCode}
                                onChange={e => setResetCode(e.target.value)}
                            />
                        </div>

                        <div className="input-group">
                            <label htmlFor="new-password">New Password</label>
                            <input
                                type="password"
                                id="new-password"
                                required
                                placeholder="Min. 6 characters"
                                value={newPassword}
                                onChange={e => setNewPassword(e.target.value)}
                            />
                        </div>

                        <div className="input-group">
                            <label htmlFor="confirm-password">Confirm New Password</label>
                            <input
                                type="password"
                                id="confirm-password"
                                required
                                placeholder="••••••••"
                                value={confirmPassword}
                                onChange={e => setConfirmPassword(e.target.value)}
                            />
                        </div>

                        <button type="submit" className="btn-primary" disabled={loading} style={{ width: '100%' }}>
                            {loading ? 'Resetting Password...' : 'Reset Password'}
                        </button>

                        {coldStartNotice && (
                            <div className="cold-start-notice">
                                <span>⚡ Updating password on server, please hold on...</span>
                            </div>
                        )}

                        <div className="toggle-mode" style={{ display: 'flex', justifyContent: 'space-between' }}>
                            <button type="button" className="btn-link" onClick={() => switchMode('forgot')}>
                                Resend code
                            </button>
                            <button type="button" className="btn-link" onClick={() => switchMode('login')}>
                                ← Back to Sign In
                            </button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}
