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
            const symArg = encodeURIComponent(symbol);

            const [data, chartData] = await Promise.all([
                apiFetch(`/api/stocks/quote?symbol=${symArg}`),
                apiFetch(`/api/stocks/chart?symbol=${symArg}`)
            ]);

            setQuote(data);

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
    const DirIcon = isUp ? (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ marginTop: '-2px' }}>
            <polyline points="18 15 12 9 6 15"></polyline>
        </svg>
    ) : (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ marginTop: '-2px' }}>
            <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
    );

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
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="3 6 5 6 21 6"></polyline>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                    </button>
                </div>
            </div>
            <div className="price-block">
                <div className="last-price">{formatCurrency(quote.lastPrice, quote.currency || 'USD')}</div>
                <div className={`change-block flex-align-center ${changeClass}`} style={{ display: 'flex', alignItems: 'center', gap: '2px', fontWeight: 600 }}>
                    {DirIcon} {Math.abs(quote.change).toFixed(2)} ({Math.abs(quote.changePercent).toFixed(2)}%)
                </div>
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
