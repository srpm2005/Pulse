import { useState, useEffect } from 'react';
import { apiFetch } from '../services/api';

export default function AlertsTable({ onAddAlertRef }) {
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
        const interval = setInterval(fetchAlerts, 5000); // 5s Long Polling Equivalent
        return () => clearInterval(interval);
    }, []);

    const deleteAlert = async (id) => {
        try {
            await apiFetch(`/api/alerts/${id}`, { method: 'DELETE' });
            fetchAlerts();
        } catch (e) {
            console.error('Failed to delete alert');
        }
    };

    if (loading) return <div>Loading alerts...</div>;

    return (
        <div className="alerts-table-container">
            <table className="alerts-table">
                <thead>
                    <tr>
                        <th>Symbol</th>
                        <th>Target Condition</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {alerts.map(alert => (
                        <tr key={alert.id} className={alert.triggered ? 'pulse-trigger' : ''}>
                            <td style={{ fontWeight: 600 }}>{alert.symbol}</td>
                            <td>
                                <span className={alert.condition === 'ABOVE' ? 'status-positive' : 'status-negative'}>
                                    {alert.condition} {alert.targetPrice.toFixed(2)}
                                </span>
                            </td>
                            <td>
                                <span className={`badge ${alert.triggered ? 'triggered' : 'pending'}`}>
                                    {alert.triggered ? 'TRIGGERED' : 'PENDING'}
                                </span>
                            </td>
                            <td>
                                <button className="icon-btn delete" onClick={() => deleteAlert(alert.id)}>
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>

            {alerts.length === 0 && (
                <div className="empty-state">
                    <p>No active price alerts.</p>
                </div>
            )}
        </div>
    );
}
