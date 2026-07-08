import { useEffect, useRef, useState } from 'react';
import Chart from 'chart.js/auto';
import { apiFetch } from '../services/api';

export default function StockWidget({ symbol, companyName, onRemove, onSetAlert }) {
    const canvasRef = useRef(null);
    const chartInstanceRef = useRef(null);

    const [quote, setQuote] = useState(null);
    const [loading, setLoading] = useState(true);

    const fetchData = async () => {
        try {
            const data = await apiFetch(`/api/stocks/quote?symbol=${encodeURIComponent(symbol)}`);
            setQuote(data);

            const chartData = await apiFetch(`/api/stocks/chart?symbol=${encodeURIComponent(symbol)}`);
            if (chartData?.prices?.length > 0) {
                renderChart(chartData);
            }
        } catch (e) {
            console.error('Failed to fetch data for', symbol);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 15000);
        return () => clearInterval(interval);
    }, [symbol]);

    const renderChart = (data) => {
        if (!canvasRef.current) return;
        const ctx = canvasRef.current.getContext('2d');

        if (chartInstanceRef.current) {
            chartInstanceRef.current.data.labels = data.timestamps;
            chartInstanceRef.current.data.datasets[0].data = data.prices;
            chartInstanceRef.current.update();
            return;
        }

        const isUp = data.prices[data.prices.length - 1] >= data.prices[0];
        const color = isUp ? '#10b981' : '#ef4444';

        chartInstanceRef.current = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.timestamps,
                datasets: [{
                    data: data.prices,
                    borderColor: color,
                    borderWidth: 2,
                    pointRadius: 0,
                    tension: 0.1,
                    fill: false
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false }, tooltip: { enabled: false } },
                scales: {
                    x: { display: false },
                    y: {
                        display: false,
                        min: Math.min(...data.prices) * 0.995,
                        max: Math.max(...data.prices) * 1.005
                    }
                },
                layout: { padding: 0 }
            }
        });
    };

    if (loading) {
        return (
            <div className="card">
                <div className="card-header">
                    <div>
                        <span className="symbol">{symbol}</span>
                        <span className="company-name">{companyName}</span>
                    </div>
                </div>
                <div style={{ padding: '20px', textAlign: 'center' }}>Loading...</div>
            </div>
        );
    }

    if (!quote) return null;

    const isUp = quote.change >= 0;
    const changeClass = isUp ? 'positive' : 'negative';
    const arrow = isUp ? '↑' : '↓';
    const formattedChange = `${arrow} ${Math.abs(quote.change).toFixed(2)} (${Math.abs(quote.changePercent).toFixed(2)}%)`;

    const formatCurrency = (amount, currencyCode = 'USD') => {
        try {
            return new Intl.NumberFormat('en-US', { style: 'currency', currency: currencyCode }).format(amount);
        } catch (e) {
            return `$${amount.toFixed(2)}`;
        }
    };

    return (
        <div className="card">
            <div className="card-header">
                <div>
                    <span className="symbol">{symbol}</span>
                    <span className="company-name">{companyName}</span>
                </div>
                <div className="card-actions">
                    <button className="icon-btn" onClick={() => onSetAlert(symbol, companyName)} title="Set Alert">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                    </button>
                    <button className="icon-btn delete" onClick={() => onRemove(symbol)} title="Remove">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </button>
                </div>
            </div>
            <div className="price-block">
                <div className="last-price">{formatCurrency(quote.lastPrice, quote.currency || 'USD')}</div>
                <div className={`change-block ${changeClass}`}>{formattedChange}</div>
            </div>
            <div className="stats-row">
                <div>High: {quote.high.toFixed(2)}</div>
                <div>Low: {quote.low.toFixed(2)}</div>
            </div>
            <div className="chart-container" style={{ height: '100px', marginTop: '15px' }}>
                <canvas ref={canvasRef}></canvas>
            </div>
        </div>
    );
}
