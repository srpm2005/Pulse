import { useState } from 'react';
import '../dashboard.css'; // Mapped from frontend-legacy

export default function Layout({ children, userEmail, onLogout, currentView, setCurrentView }) {
    return (
        <div className="layout">
            <aside className="sidebar">
                <div className="logo-container" style={{ padding: '0 4px 24px 4px' }}>
                    <div className="logo" style={{ margin: 0, justifyContent: 'flex-start' }}>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ color: 'var(--text-primary)' }}>
                            <path d="M22 12h-4l-3 9L9 3l-3 9H2"></path>
                        </svg>
                        <span style={{ fontSize: '14px', letterSpacing: '0.5px' }}>Pulse</span>
                    </div>
                </div>

                <nav className="nav-links">
                    <button
                        className={`nav-item ${currentView === 'dashboard' ? 'active' : ''}`}
                        onClick={() => setCurrentView('dashboard')}
                        style={{ border: 'none', width: '100%', textAlign: 'left', cursor: 'pointer', fontFamily: 'inherit' }}
                    >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ marginRight: '12px' }}>
                            <rect x="3" y="3" width="7" height="9"></rect>
                            <rect x="14" y="3" width="7" height="5"></rect>
                            <rect x="14" y="12" width="7" height="9"></rect>
                            <rect x="3" y="16" width="7" height="5"></rect>
                        </svg>
                        Dashboard
                    </button>
                    <button
                        className={`nav-item ${currentView === 'alerts' ? 'active' : ''}`}
                        onClick={() => setCurrentView('alerts')}
                        style={{ border: 'none', width: '100%', textAlign: 'left', cursor: 'pointer', fontFamily: 'inherit' }}
                    >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ marginRight: '12px' }}>
                            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                        </svg>
                        Alerts
                    </button>
                </nav>

                <div className="user-block">
                    <div className="user-email">{userEmail || 'user@example.com'}</div>
                    <button className="btn-logout" onClick={onLogout}>Logout</button>
                </div>
            </aside>

            <main className="content-area">
                {children}
            </main>
        </div>
    );
}
