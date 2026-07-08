import '../dashboard.css';

export default function Layout({ children, userEmail, onLogout, currentView, setCurrentView }) {
    return (
        <div className="layout">
            <aside className="sidebar">
                <div className="sidebar-logo">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M22 12h-4l-3 9L9 3l-3 9H2"></path>
                    </svg>
                    <span>PULSE</span>
                </div>

                {/* <div className="sidebar-section-label">NAVIGATION</div> */}

                <nav className="nav-links">
                    <button
                        className={`nav-item ${currentView === 'dashboard' ? 'active' : ''}`}
                        onClick={() => setCurrentView('dashboard')}
                    >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <rect x="3" y="3" width="7" height="9"></rect>
                            <rect x="14" y="3" width="7" height="5"></rect>
                            <rect x="14" y="12" width="7" height="9"></rect>
                            <rect x="3" y="16" width="7" height="5"></rect>
                        </svg>
                        <span>Dashboard</span>
                    </button>
                    <button
                        className={`nav-item ${currentView === 'alerts' ? 'active' : ''}`}
                        onClick={() => setCurrentView('alerts')}
                    >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                        </svg>
                        <span>Alerts</span>
                    </button>
                </nav>

                <div className="user-block">
                    <div className="sidebar-section-label">ACCOUNT</div>
                    <div className="user-email">{userEmail || 'user@example.com'}</div>
                    <button className="btn-logout" onClick={onLogout}>Sign out</button>
                </div>
            </aside>

            <main className="content-area">
                {children}
            </main>
        </div>
    );
}
