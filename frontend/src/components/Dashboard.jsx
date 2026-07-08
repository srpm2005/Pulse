import { useState, useEffect } from 'react';
import { apiFetch } from '../services/api';
import Layout from './Layout';
import StockWidget from './StockWidget';
import AlertsTable from './AlertsTable';
import SearchModal from './SearchModal';
import AlertModal from './AlertModal';

export default function DashboardContainer({ onLogout }) {
    const [currentView, setCurrentView] = useState('dashboard');
    const [trackedStocks, setTrackedStocks] = useState([]);

    const [isAlertOpen, setIsAlertOpen] = useState(false);
    const [alertDefaultSymbol, setAlertDefaultSymbol] = useState('');
    const [userEmail, setUserEmail] = useState(() => {
        const token = localStorage.getItem('pulse_jwt');
        if (token) {
            try {
                const payloadUrl = token.split('.')[1];
                const base64 = payloadUrl.replace(/-/g, '+').replace(/_/g, '/');
                const jsonPayload = decodeURIComponent(atob(base64).split('').map(function (c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));

                const decoded = JSON.parse(jsonPayload);
                return decoded.sub || "user@pulse.com";
            } catch (e) {
                return "user@pulse.com";
            }
        }
        return "user@pulse.com";
    });

    useEffect(() => {
        const fetchTracked = async () => {
            try {
                const data = await apiFetch('/api/stocks/tracked');
                setTrackedStocks(data);
            } catch (e) {
                console.error(e);
            }
        };
        fetchTracked();
    }, []);

    const handleAddStock = async (instrumentKey, symbol, companyName) => {
        if (trackedStocks.find(s => s.instrumentKey === instrumentKey || s.symbol === symbol)) return;
        try {
            const newStock = await apiFetch('/api/stocks/tracked', {
                method: 'POST',
                body: JSON.stringify({ symbol, companyName })
            });
            setTrackedStocks([...trackedStocks, newStock]);
        } catch (e) {
            console.error(e);
        }
    };

    const removeStock = async (symbol) => {
        try {
            await apiFetch(`/api/stocks/tracked/${symbol}`, { method: 'DELETE' });
            setTrackedStocks(trackedStocks.filter(s => s.symbol !== symbol));
        } catch (e) {
            console.error(e);
        }
    };

    const handleOpenAlertFromCard = (symbol, companyName) => {
        setCurrentView('alerts');
        setAlertDefaultSymbol(symbol);
        setIsAlertOpen(true);
    };

    return (
        <>
            <Layout userEmail={userEmail} onLogout={onLogout} currentView={currentView} setCurrentView={setCurrentView}>
                {currentView === 'dashboard' ? (
                    <section className="view-section" style={{ paddingTop: '72px' }}>

                        <div className="stock-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '20px' }}>
                            {trackedStocks.length === 0 && (
                                <div className="empty-state" style={{ gridColumn: '1 / -1' }}>
                                    <p>You aren't tracking any stocks.</p>
                                    <button className="btn-link" onClick={() => setIsSearchOpen(true)}>Add your first stock</button>
                                </div>
                            )}
                            {trackedStocks.map(stock => (
                                <StockWidget
                                    key={stock.instrumentKey}
                                    symbol={stock.symbol}
                                    companyName={stock.companyName}
                                    onRemove={removeStock}
                                    onSetAlert={handleOpenAlertFromCard}
                                />
                            ))}
                        </div>
                    </section>
                ) : (
                    <section className="view-section">
                        <header className="view-header" style={{ justifyContent: 'flex-end', borderBottom: 'none' }}>
                            <button className="btn-primary btn-sm" style={{ display: 'flex', alignItems: 'center', gap: '6px' }} onClick={() => { setAlertDefaultSymbol(''); setIsAlertOpen(true); }}>
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                    <line x1="12" y1="5" x2="12" y2="19"></line>
                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                </svg>
                                Create Alert
                            </button>
                        </header>
                        <AlertsTable />
                    </section>
                )}
            </Layout>

            <SearchModal
                isOpen={currentView === 'dashboard'}
                onClose={() => { }}
                onAddStock={handleAddStock}
            />

            <AlertModal
                isOpen={isAlertOpen}
                onClose={() => setIsAlertOpen(false)}
                defaultSymbol={alertDefaultSymbol}
                onAlertCreated={() => {
                    // Alert table polls internally
                }}
            />
        </>
    );
}
