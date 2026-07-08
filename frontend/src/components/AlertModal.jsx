import { useState, useEffect } from 'react';
import { apiFetch } from '../services/api';

export default function AlertModal({ isOpen, onClose, defaultSymbol, onAlertCreated }) {
    const [symbol, setSymbol] = useState('');
    const [condition, setCondition] = useState('ABOVE');
    const [price, setPrice] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (isOpen && defaultSymbol) {
            setSymbol(defaultSymbol);
        }
    }, [isOpen, defaultSymbol]);

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            await apiFetch('/api/alerts', {
                method: 'POST',
                body: JSON.stringify({
                    symbol: symbol.toUpperCase(),
                    targetPrice: parseFloat(price),
                    condition: condition
                })
            });
            setSymbol('');
            setPrice('');
            onAlertCreated(); // Refresh alerts
            onClose();
        } catch (err) {
            // Error handled globally via toast usually
            alert('Failed to set alert');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="modal-backdrop">
            <div className="modal">
                <div className="modal-header">
                    <h3>Set Price Alert</h3>
                    <button className="btn-close" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body">
                    <form onSubmit={handleSubmit}>
                        <div className="input-group">
                            <label>Symbol</label>
                            <input
                                type="text"
                                required
                                placeholder="e.g. RELIANCE"
                                value={symbol}
                                onChange={e => setSymbol(e.target.value)}
                            />
                        </div>

                        <div className="input-row" style={{ marginTop: '16px' }}>
                            <div className="input-group">
                                <label>Condition</label>
                                <select
                                    className="form-select"
                                    value={condition}
                                    onChange={e => setCondition(e.target.value)}
                                >
                                    <option value="ABOVE">Above</option>
                                    <option value="BELOW">Below</option>
                                </select>
                            </div>

                            <div className="input-group">
                                <label>Target Price</label>
                                <input
                                    type="number"
                                    step="0.01"
                                    required
                                    placeholder="e.g. 2500.00"
                                    value={price}
                                    onChange={e => setPrice(e.target.value)}
                                />
                            </div>
                        </div>

                        <button
                            type="submit"
                            className="btn-primary"
                            style={{ marginTop: '24px', width: '100%' }}
                            disabled={loading}
                        >
                            {loading ? 'Saving...' : 'Save Alert'}
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
}
