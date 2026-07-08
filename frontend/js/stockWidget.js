// In-memory tracked stocks
let trackedStocks = JSON.parse(localStorage.getItem('pulse_tracked_stocks')) || [];
let refreshInterval;

window.initStocks = () => {
    document.getElementById('btn-add-stock').addEventListener('click', openSearchModal);

    const searchInput = document.getElementById('search-input');
    let timeout;
    searchInput.addEventListener('input', (e) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => {
            searchSymbol(e.target.value);
        }, 300);
    });

    renderAllCards();
    startAutoRefresh();
};

const openSearchModal = () => {
    document.getElementById('search-input').value = '';
    document.getElementById('search-results').innerHTML = '';
    openModal('modal-search');
};

const searchSymbol = async (query) => {
    const resultsContainer = document.getElementById('search-results');
    if (!query || query.length < 2) {
        resultsContainer.innerHTML = '';
        return;
    }

    resultsContainer.innerHTML = '<div style="padding: 12px; text-align:center;">Searching...</div>';

    try {
        const results = await apiFetch(`/api/stocks/search?q=${encodeURIComponent(query)}`);

        if (results.length === 0) {
            resultsContainer.innerHTML = '<div style="padding: 12px; text-align:center; color: var(--text-secondary);">No results found</div>';
            return;
        }

        resultsContainer.innerHTML = results.map(item => `
            <div class="search-item" onclick="addStock('${item.instrumentKey}', '${item.symbol}', '${item.companyName.replace(/'/g, "\\'")}')">
                <div>
                    <div style="font-weight: 600;">${item.symbol}</div>
                    <div style="font-size: 0.8rem; color: var(--text-secondary);">${item.companyName}</div>
                </div>
                <span>+</span>
            </div>
        `).join('');
    } catch {
        resultsContainer.innerHTML = '<div style="padding: 12px; text-align:center; color: var(--danger);">Search failed</div>';
    }
};

window.addStock = (instrumentKey, symbol, companyName) => {
    if (trackedStocks.some(s => s.instrumentKey === instrumentKey)) {
        showToast('Stock already tracked', 'error');
        return;
    }

    trackedStocks.push({ instrumentKey, symbol, companyName });
    localStorage.setItem('pulse_tracked_stocks', JSON.stringify(trackedStocks));

    closeModal('modal-search');
    showToast(`${symbol} added to dashboard`, 'success');

    renderCardSkeleton(instrumentKey);
    fetchAndRenderQuote(instrumentKey);
    checkEmptyState();
};

window.removeStock = (instrumentKey) => {
    trackedStocks = trackedStocks.filter(s => s.instrumentKey !== instrumentKey);
    localStorage.setItem('pulse_tracked_stocks', JSON.stringify(trackedStocks));

    const card = document.getElementById(`stock-card-${instrumentKey}`);
    if (card) card.remove();

    showToast(`Removed from dashboard`, 'success');
    checkEmptyState();
};

window.openAlertsFromCard = (symbol, companyName) => {
    document.querySelector('.nav-item[data-target="alerts-view"]').click();
    setTimeout(() => {
        document.getElementById('btn-create-alert').click();
        document.getElementById('alert-symbol').value = symbol;
        document.getElementById('alert-company').value = companyName;
    }, 100);
};

const checkEmptyState = () => {
    const emptyState = document.getElementById('empty-stocks');
    if (trackedStocks.length === 0) {
        emptyState.style.display = 'block';
    } else {
        emptyState.style.display = 'none';
    }
};

const renderAllCards = () => {
    checkEmptyState();

    // Clear grid except empty state
    const grid = document.getElementById('stock-grid');
    Array.from(grid.children).forEach(child => {
        if (child.id !== 'empty-stocks') child.remove();
    });

    trackedStocks.forEach(stock => {
        renderCardSkeleton(stock.instrumentKey);
        fetchAndRenderQuote(stock.instrumentKey);
    });
};

const renderCardSkeleton = (instrumentKey) => {
    const grid = document.getElementById('stock-grid');
    const existing = document.getElementById(`stock-card-${instrumentKey}`);
    if (existing) return;

    const stock = trackedStocks.find(s => s.instrumentKey === instrumentKey);

    const d = document.createElement('div');
    d.className = 'card';
    d.id = `stock-card-${instrumentKey}`;
    d.innerHTML = `
        <div class="card-header">
            <div>
                <span class="symbol">${stock.symbol || stock.instrumentKey}</span>
                <span class="company-name">${stock.companyName || ''}</span>
            </div>
            <div class="card-actions">
                <button class="icon-btn" onclick="openAlertsFromCard('${stock.symbol || stock.instrumentKey}', '${(stock.companyName || '').replace(/'/g, "\\'")}')" title="Set Alert">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                </button>
                <button class="icon-btn delete" onclick="removeStock('${instrumentKey}')" title="Remove">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                </button>
            </div>
        </div>
        <div class="price-block">
            <div class="last-price">...</div>
            <div class="change-block">...</div>
        </div>
        <div class="stats-row">
            <div>High: --</div>
            <div>Low: --</div>
        </div>
        <div class="chart-container" style="height: 100px; margin-top: 15px;">
            <canvas id="chart-${instrumentKey}"></canvas>
        </div>
    `;

    grid.appendChild(d);
};

const fetchAndRenderQuote = async (instrumentKey) => {
    try {
        const quote = await apiFetch(`/api/stocks/quote?symbol=${encodeURIComponent(instrumentKey)}`);
        if (!quote) return;

        const card = document.getElementById(`stock-card-${instrumentKey}`);
        if (!card) return;

        const isUp = quote.change >= 0;
        const changeClass = isUp ? 'positive' : 'negative';
        const arrow = isUp ? '↑' : '↓';
        const formattedChange = `${arrow} ${Math.abs(quote.change).toFixed(2)} (${Math.abs(quote.changePercent).toFixed(2)}%)`;

        card.querySelector('.last-price').textContent = quote.lastPrice.toFixed(2);

        const changeBlock = card.querySelector('.change-block');
        changeBlock.className = `change-block ${changeClass}`;
        changeBlock.textContent = formattedChange;

        const stats = card.querySelectorAll('.stats-row div');
        stats[0].textContent = `High: ${quote.high.toFixed(2)}`;
        stats[1].textContent = `Low: ${quote.low.toFixed(2)}`;

        // Load historical chart data
        const chartData = await apiFetch(`/api/stocks/chart?symbol=${encodeURIComponent(instrumentKey)}`);
        if (chartData && chartData.prices && chartData.prices.length > 0) {
            renderChart(instrumentKey, chartData);
        }
    } catch (e) {
        console.error('Failed to fetch quote for', instrumentKey);
    }
};

const charts = {};

const renderChart = (instrumentKey, data) => {
    const ctx = document.getElementById(`chart-${instrumentKey}`);
    if (!ctx) return;

    if (charts[instrumentKey]) {
        charts[instrumentKey].data.labels = data.timestamps;
        charts[instrumentKey].data.datasets[0].data = data.prices;
        charts[instrumentKey].update();
        return;
    }

    const isUp = data.prices[data.prices.length - 1] >= data.prices[0];
    const color = isUp ? '#10b981' : '#ef4444';

    charts[instrumentKey] = new Chart(ctx, {
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
                y: { display: false, min: Math.min(...data.prices) * 0.995, max: Math.max(...data.prices) * 1.005 }
            },
            layout: { padding: 0 }
        }
    });
};

const startAutoRefresh = () => {
    if (refreshInterval) clearInterval(refreshInterval);
    refreshInterval = setInterval(() => {
        trackedStocks.forEach(stock => fetchAndRenderQuote(stock.instrumentKey));
    }, 15000); // 15 seconds real-time fast poll
};
