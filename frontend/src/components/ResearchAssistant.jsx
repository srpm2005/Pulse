import React, { useState, useEffect, useRef } from 'react';
import { streamResearch, getResearchPresets } from '../services/api';
import '../research.css';

const DEFAULT_PRESETS = [
    {
        title: "AAPL Quantitative & SMA Analysis",
        query: "Analyze AAPL: calculate its 20-day SMA, 52-week position, and short-term volatility."
    },
    {
        title: "NVDA vs TSLA Momentum",
        query: "Compare NVDA and TSLA 14-day RSI, 30-day momentum, and relative trend strength."
    },
    {
        title: "Portfolio Risk & Price Alerts",
        query: "Perform a quantitative audit of my portfolio watchlist and check proximity to active price alerts."
    },
    {
        title: "MSFT Volatility & Mean Reversion",
        query: "Analyze MSFT 30-day realized volatility and evaluate mean-reversion risk."
    }
];

export default function ResearchAssistant({ isOpen, onClose, initialSymbol = null }) {
    const [query, setQuery] = useState('');
    const [isStreaming, setIsStreaming] = useState(false);
    const [statusMessage, setStatusMessage] = useState('Idle');
    const [streamedContent, setStreamedContent] = useState('');
    const [groundedDocs, setGroundedDocs] = useState([]);
    const [citations, setCitations] = useState([]);
    const [presets, setPresets] = useState(DEFAULT_PRESETS);
    const [highlightedDocId, setHighlightedDocId] = useState(null);
    const [copySuccess, setCopySuccess] = useState(false);

    const streamCloseRef = useRef(null);
    const contentEndRef = useRef(null);
    const docsContainerRef = useRef(null);

    useEffect(() => {
        if (isOpen) {
            getResearchPresets()
                .then(res => {
                    if (res && Array.isArray(res) && res.length > 0) {
                        setPresets(res);
                    }
                })
                .catch(() => {});

            if (initialSymbol) {
                const autoQuery = `Analyze ${initialSymbol}: calculate its 20-day SMA, 14-day RSI, 52-week position, and realized volatility.`;
                setQuery(autoQuery);
                executeResearch(autoQuery, [initialSymbol]);
            }
        }
        return () => {
            if (streamCloseRef.current) {
                streamCloseRef.current();
            }
        };
    }, [isOpen, initialSymbol]);

    // Auto-scroll stream content
    useEffect(() => {
        if (isStreaming && contentEndRef.current) {
            contentEndRef.current.scrollIntoView({ behavior: 'smooth' });
        }
    }, [streamedContent, isStreaming]);

    if (!isOpen) return null;

    const executeResearch = (searchQuery, symbols = []) => {
        if (!searchQuery || !searchQuery.trim() || isStreaming) return;

        if (streamCloseRef.current) {
            streamCloseRef.current();
        }

        setIsStreaming(true);
        setStatusMessage('Connecting to quantitative telemetry stream...');
        setStreamedContent('');
        setGroundedDocs([]);
        setCitations([]);
        setHighlightedDocId(null);
        setCopySuccess(false);

        const closeStream = streamResearch({
            query: searchQuery,
            symbols,
            onStatus: (msg) => {
                setStatusMessage(msg);
            },
            onContext: (contextData) => {
                if (contextData?.documents) {
                    setGroundedDocs(contextData.documents);
                }
                if (contextData?.citations) {
                    setCitations(contextData.citations);
                }
            },
            onChunk: (chunk) => {
                setStreamedContent(prev => prev + chunk);
            },
            onComplete: () => {
                setIsStreaming(false);
                setStatusMessage('Grounded synthesis complete');
            },
            onError: (err) => {
                console.error('SSE Error:', err);
                setIsStreaming(false);
                setStatusMessage('Stream error or server disconnected');
            }
        });

        streamCloseRef.current = closeStream;
    };

    const handleFormSubmit = (e) => {
        e.preventDefault();
        executeResearch(query);
    };

    const handlePresetClick = (preset) => {
        setQuery(preset.query);
        executeResearch(preset.query);
    };

    const handleCitationClick = (docId) => {
        setHighlightedDocId(docId);
        // Scroll to card in right pane
        const el = document.getElementById(`doc-card-${docId}`);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    };

    const handleCopyReport = () => {
        if (!streamedContent) return;
        navigator.clipboard.writeText(streamedContent)
            .then(() => {
                setCopySuccess(true);
                setTimeout(() => setCopySuccess(false), 2000);
            })
            .catch(() => {});
    };

    // Render formatted markdown with interactive clickable citations
    const renderFormattedStream = (rawText) => {
        if (!rawText) return null;

        const lines = rawText.split('\n');
        return lines.map((line, idx) => {
            // Headings
            if (line.startsWith('### ')) {
                return <h3 key={idx}>{line.substring(4)}</h3>;
            }
            if (line.startsWith('#### ')) {
                return <h4 key={idx}>{line.substring(5)}</h4>;
            }
            if (line.startsWith('##### ')) {
                return <h5 key={idx}>{line.substring(6)}</h5>;
            }
            if (line.startsWith('---')) {
                return <hr key={idx} />;
            }
            if (line.startsWith('> ')) {
                return <blockquote key={idx}>{renderLineWithCitations(line.substring(2))}</blockquote>;
            }
            if (line.startsWith('* ') || line.startsWith('- ')) {
                return (
                    <ul key={idx} style={{ margin: '0.25rem 0' }}>
                        <li>{renderLineWithCitations(line.substring(2))}</li>
                    </ul>
                );
            }
            if (!line.trim()) {
                return <div key={idx} style={{ height: '0.5rem' }} />;
            }
            return <p key={idx} style={{ margin: '0.4rem 0' }}>{renderLineWithCitations(line)}</p>;
        });
    };

    // Replace [Source: DOC-X] or [DOC-X] with clickable citation badges
    const renderLineWithCitations = (line) => {
        const parts = line.split(/(\[(?:Source:\s*)?DOC-\d+\])/g);
        return parts.map((part, pIdx) => {
            const match = part.match(/\[(?:Source:\s*)?(DOC-\d+)\]/);
            if (match) {
                const docId = match[1];
                const isActive = highlightedDocId === docId;
                return (
                    <button
                        key={pIdx}
                        type="button"
                        className={`citation-badge ${isActive ? 'active' : ''}`}
                        onClick={() => handleCitationClick(docId)}
                        title={`Click to inspect ground truth telemetry (${docId})`}
                    >
                        ⚡ {docId}
                    </button>
                );
            }

            // Bold styling
            const boldParts = part.split(/(\*\*[^*]+\*\*)/g);
            return boldParts.map((bPart, bIdx) => {
                if (bPart.startsWith('**') && bPart.endsWith('**')) {
                    return <strong key={bIdx} style={{ color: '#fff' }}>{bPart.slice(2, -2)}</strong>;
                }
                return bPart;
            });
        });
    };

    return (
        <div className="research-modal-overlay" onClick={onClose}>
            <div className="research-modal-container" onClick={(e) => e.stopPropagation()}>
                {/* Header */}
                <div className="research-header">
                    <div className="research-title-group">
                        <span className="research-title">
                            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
                            </svg>
                            Quantitative Research
                        </span>
                        <span className="research-status-pill">
                            <span className={isStreaming ? 'pulse-dot' : ''}></span>
                            {isStreaming ? 'Live SSE Stream' : 'RAG Grounded'}
                        </span>
                    </div>
                    <button className="btn-close" onClick={onClose} title="Close">✕</button>
                </div>

                {/* Controls Bar */}
                <div className="research-controls">
                    <form className="research-input-wrapper" onSubmit={handleFormSubmit}>
                        <input
                            type="text"
                            className="research-input"
                            placeholder="Ask any quantitative query (e.g., 'Analyze AAPL SMA20 & RSI', 'Compare NVDA vs TSLA momentum')..."
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            disabled={isStreaming}
                        />
                        <button type="submit" className="btn-primary btn-sm" disabled={isStreaming || !query.trim()}>
                            {isStreaming ? 'Streaming...' : 'Run Research'}
                        </button>
                    </form>

                    <div className="presets-container">
                        <span className="presets-label">Presets:</span>
                        {presets.map((p, i) => (
                            <button
                                key={i}
                                type="button"
                                className="preset-chip"
                                onClick={() => handlePresetClick(p)}
                                disabled={isStreaming}
                            >
                                {p.title}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Main Split View */}
                <div className="research-body">
                    {/* Left Pane: Live Stream */}
                    <div className="research-stream-pane">
                        <div className="pane-header">
                            <span className="stream-status-ticker">
                                {isStreaming && <span className="pulse-dot"></span>}
                                {statusMessage}
                            </span>
                            {streamedContent && (
                                <button className="pane-action-btn" onClick={handleCopyReport}>
                                    {copySuccess ? 'Copied' : 'Copy'}
                                </button>
                            )}
                        </div>

                        <div className="stream-content">
                            {streamedContent ? (
                                <>
                                    {renderFormattedStream(streamedContent)}
                                    {isStreaming && <span className="typing-cursor"></span>}
                                    <div ref={contentEndRef} />
                                </>
                            ) : (
                                <div className="research-empty-state">
                                    <svg className="empty-icon-svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                                        <line x1="18" y1="20" x2="18" y2="10"></line>
                                        <line x1="12" y1="20" x2="12" y2="4"></line>
                                        <line x1="6" y1="20" x2="6" y2="14"></line>
                                    </svg>
                                    <p className="empty-hint">
                                        Enter a ticker or click a preset to stream a grounded quantitative report.
                                    </p>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Right Pane: Grounded Documents & Real-Time Telemetry */}
                    <div className="research-context-pane">
                        <div className="pane-header">
                            <span>Grounded Context ({groundedDocs.length})</span>
                            <span style={{ fontSize: '10px', color: 'var(--text-muted)' }}>Telemetry</span>
                        </div>

                        <div className="documents-list" ref={docsContainerRef}>
                            {groundedDocs.length > 0 ? (
                                groundedDocs.map((doc) => {
                                    const isHighlighted = highlightedDocId === doc.id;
                                    return (
                                        <div
                                            key={doc.id}
                                            id={`doc-card-${doc.id}`}
                                            className={`doc-card ${isHighlighted ? 'highlighted' : ''}`}
                                        >
                                            <div className="doc-card-header">
                                                <span className="doc-tag">[{doc.id}]</span>
                                                <span className="doc-source">{doc.source}</span>
                                            </div>
                                            <div className="doc-title">{doc.title}</div>
                                            <div className="doc-content-snippet">{doc.content}</div>

                                            {doc.metrics && Object.keys(doc.metrics).length > 0 && (
                                                <div className="doc-metrics-grid">
                                                    {Object.entries(doc.metrics).map(([k, v]) => (
                                                        <div key={k} className="doc-metric-item">
                                                            <div className="doc-metric-key">{k}</div>
                                                            <div className="doc-metric-val">
                                                                {typeof v === 'number'
                                                                    ? (Number.isInteger(v) ? v.toLocaleString() : v.toFixed(2))
                                                                    : String(v)}
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            )}
                                        </div>
                                    );
                                })
                            ) : (
                                <div className="research-empty-state">
                                    <svg className="empty-icon-svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                                        <ellipse cx="12" cy="5" rx="9" ry="3"></ellipse>
                                        <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"></path>
                                        <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"></path>
                                    </svg>
                                    <p className="empty-hint">
                                        Grounded market telemetry, calculated technical indicators, and active alerts will populate here.
                                    </p>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
