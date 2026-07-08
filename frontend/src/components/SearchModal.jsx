import { useState } from 'react';
import { apiFetch } from '../services/api';

export default function SearchModal({ isOpen, onClose, onAddStock }) {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [searching, setSearching] = useState(false);
    const [error, setError] = useState(null);

    if (!isOpen) return null;

    const handleSearch = async (e) => {
        const val = e.target.value;
        setQuery(val);

        if (val.length < 2) {
            setResults([]);
            return;
        }

        setSearching(true);
        setError(null);

        try {
            const data = await apiFetch(`/api/stocks/search?q=${encodeURIComponent(val)}`);
            setResults(data);
        } catch (err) {
            setError('Search failed');
            setResults([]);
        } finally {
            setSearching(false);
        }
    };

    const handleSelect = (item) => {
        onAddStock(item.instrumentKey, item.symbol, item.companyName);
        setQuery('');
        setResults([]);
        onClose();
    };

    return (
        <div className="modal-backdrop">
            <div className="modal">
                <div className="modal-header">
                    <h3>Search Stock</h3>
                    <button className="btn-close" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body">
                    <input
                        type="text"
                        placeholder="Search symbol or company name..."
                        value={query}
                        onChange={handleSearch}
                        autoFocus
                    />
                    <div className="search-results">
                        {searching && <div style={{ padding: '12px', textAlign: 'center' }}>Searching...</div>}
                        {!searching && error && <div style={{ padding: '12px', textAlign: 'center', color: 'var(--danger)' }}>{error}</div>}
                        {!searching && !error && query.length >= 2 && results.length === 0 && (
                            <div style={{ padding: '12px', textAlign: 'center', color: 'var(--text-secondary)' }}>No results found</div>
                        )}
                        {results.map(item => (
                            <div key={item.instrumentKey} className="search-item" onClick={() => handleSelect(item)}>
                                <div>
                                    <div style={{ fontWeight: 600 }}>{item.symbol}</div>
                                    <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{item.companyName}</div>
                                </div>
                                <span style={{ color: 'var(--text-secondary)' }}>+</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}
