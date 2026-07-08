import { useState, useEffect } from 'react';
import { apiFetch } from '../services/api';

export default function AlertsTable() {
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchAlerts = async () => {
        try {
            const data = await apiFetch('/api/alerts');
            setAlerts(data);
        } catch (e) {
            console.error('Failed to load alerts');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAlerts();
        const interval = setInterval(fetchAlerts, 5000);
        return () => clearInterval(interval);
    }, []);

    const deleteAlert = async (id) => {
        try {
            await apiFetch(`/api/alerts/${id}`, { method: 'DELETE' });
            setAlerts(prev => prev.filter(a => a.id !== id));
        } catch (e) {
            console.error('Failed to delete alert');
        }
    };

    if (loading) return (
        <div className="alerts-loading">
            <span className="alerts-loading-dot" />
            Loading alerts…
        </div>
    );

    if (alerts.length === 0) return (
        <div className="alerts-empty">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
            </svg>
            <p>No price alerts set</p>
            <span>Use "+ Create Alert" to monitor a stock</span>
        </div>
    );

    const triggered = alerts.filter(a => a.triggered);
    const pending = alerts.filter(a => !a.triggered);

    return (
        <div className="alerts-layout">
            {triggered.length > 0 && (
                <div className="alerts-group">
                    <div className="alerts-group-label">TRIGGERED</div>
                    {triggered.map(alert => <AlertCard key={alert.id} alert={alert} onDelete={deleteAlert} />)}
                </div>
            )}
            {pending.length > 0 && (
                <div className="alerts-group">
                    <div className="alerts-group-label">WATCHING</div>
                    {pending.map(alert => <AlertCard key={alert.id} alert={alert} onDelete={deleteAlert} />)}
                </div>
            )}
        </div>
    );
}

function AlertCard({ alert, onDelete }) {
    const isAbove = alert.condition === 'ABOVE';
    return (
        <div className={`alert-card ${alert.triggered ? 'alert-card--triggered' : ''}`}>
            <div className="alert-card-left">
                <div className="alert-card-symbol">{alert.symbol}</div>
                {alert.companyName && <div className="alert-card-company">{alert.companyName}</div>}
            </div>
            <div className="alert-card-condition">
                <span className={`alert-dir ${isAbove ? 'alert-dir--above' : 'alert-dir--below'}`}>
                    {isAbove ? (
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ marginRight: '4px' }}>
                            <polyline points="18 15 12 9 6 15"></polyline>
                        </svg>
                    ) : (
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ marginRight: '4px' }}>
                            <polyline points="6 9 12 15 18 9"></polyline>
                        </svg>
                    )}
                    {alert.condition}
                </span>
                <span className="alert-price">{Number(alert.targetPrice).toFixed(2)}</span>
            </div>
            <div className="alert-card-right">
                <span className={`badge ${alert.triggered ? 'triggered' : 'pending'}`}>
                    {alert.triggered ? 'TRIGGERED' : 'PENDING'}
                </span>
                <button className="icon-btn delete" onClick={() => onDelete(alert.id)} title="Delete alert">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                </button>
            </div>
        </div>
    );
}
