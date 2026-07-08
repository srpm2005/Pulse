import { useState, useEffect, useRef } from 'react';
import { apiFetch } from '../services/api';
import '../searchbar.css';

export default function StockSearchBar({ isOpen, onAddStock }) {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [searching, setSearching] = useState(false);
    const [activeIndex, setActiveIndex] = useState(-1);
    const [isFocused, setIsFocused] = useState(false);
    const inputRef = useRef(null);
    const debounceRef = useRef(null);

    useEffect(() => {
        if (isOpen) {
            setQuery('');
            setResults([]);
            setActiveIndex(-1);
            setTimeout(() => inputRef.current?.focus(), 50);
        }
    }, [isOpen]);

    useEffect(() => {
        clearTimeout(debounceRef.current);
        if (query.length < 2) { setResults([]); setSearching(false); return; }

        setSearching(true);
        debounceRef.current = setTimeout(async () => {
            try {
                const data = await apiFetch(`/api/stocks/search?q=${encodeURIComponent(query)}`);
                setResults(data || []);
                setActiveIndex(-1);
            } catch {
                setResults([]);
            } finally {
                setSearching(false);
            }
        }, 300);

        return () => clearTimeout(debounceRef.current);
    }, [query]);

    const dismiss = () => {
        setQuery('');
        setResults([]);
        setIsFocused(false);
    };

    const handleSelect = (item) => {
        onAddStock(item.instrumentKey, item.symbol, item.companyName);
        dismiss();
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Escape') { dismiss(); return; }
        if (e.key === 'ArrowDown') { setActiveIndex(i => Math.min(i + 1, results.length - 1)); e.preventDefault(); }
        if (e.key === 'ArrowUp') { setActiveIndex(i => Math.max(i - 1, 0)); e.preventDefault(); }
        if (e.key === 'Enter' && activeIndex >= 0) { handleSelect(results[activeIndex]); }
    };

    if (!isOpen) return null;

    const showDropdown = isFocused && query.length >= 2;

    return (
        <>
            {showDropdown && <div className="searchbar-backdrop" onMouseDown={dismiss} />}
            <div className="searchbar-container">
                <div className="searchbar-input-row">
                    <svg className="searchbar-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                    <input
                        ref={inputRef}
                        type="text"
                        className="searchbar-input"
                        placeholder="Search symbol or company..."
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                        onKeyDown={handleKeyDown}
                        onFocus={() => setIsFocused(true)}
                    />
                    {searching && <span className="searchbar-spinner" />}
                    {query && !searching && (
                        <button className="searchbar-clear" onMouseDown={e => { e.preventDefault(); dismiss(); }}>✕</button>
                    )}
                    <kbd className="searchbar-esc" onMouseDown={e => { e.preventDefault(); dismiss(); }}>esc</kbd>
                </div>

                {showDropdown && results.length > 0 && (
                    <div className="searchbar-results">
                        {results.map((item, i) => (
                            <div
                                key={item.instrumentKey}
                                className={`searchbar-result-item ${i === activeIndex ? 'active' : ''}`}
                                onMouseDown={() => handleSelect(item)}
                                onMouseEnter={() => setActiveIndex(i)}
                            >
                                <span className="result-symbol">{item.symbol}</span>
                                <span className="result-name">{item.companyName}</span>
                                <span className="result-add">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <circle cx="12" cy="12" r="10"></circle>
                                        <line x1="12" y1="8" x2="12" y2="16"></line>
                                        <line x1="8" y1="12" x2="16" y2="12"></line>
                                    </svg>
                                </span>
                            </div>
                        ))}
                    </div>
                )}

                {showDropdown && !searching && results.length === 0 && (
                    <div className="searchbar-empty">No results for "<strong>{query}</strong>"</div>
                )}
            </div>
        </>
    );
}
