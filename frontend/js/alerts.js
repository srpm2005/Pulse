document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('btn-create-alert').addEventListener('click', () => {
        document.getElementById('form-create-alert').reset();
        document.getElementById('alert-company').value = '';
        openModal('modal-alert');
    });

    document.getElementById('form-create-alert').addEventListener('submit', async (e) => {
        e.preventDefault();

        const payload = {
            symbol: document.getElementById('alert-symbol').value,
            companyName: document.getElementById('alert-company').value || null,
            targetPrice: parseFloat(document.getElementById('alert-price').value),
            condition: document.getElementById('alert-condition').value
        };

        try {
            await apiFetch('/api/alerts', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            showToast('Alert created successfully', 'success');
            closeModal('modal-alert');
            window.loadAlerts();
        } catch (error) {
            // Toast automatically shown
        }
    });

    // Make load alerts globally available
    window.loadAlerts = async () => {
        showLoader();
        try {
            const alerts = await apiFetch('/api/alerts');
            renderAlerts(alerts);
        } catch (error) {
            // Already handled
        } finally {
            hideLoader();
        }
    };
});

const renderAlerts = (alerts) => {
    const tbody = document.getElementById('alerts-tbody');
    const emptyState = document.getElementById('empty-alerts');

    if (alerts.length === 0) {
        tbody.innerHTML = '';
        emptyState.classList.remove('hidden');
        return;
    }

    emptyState.classList.add('hidden');

    tbody.innerHTML = alerts.map(alert => `
        <tr>
            <td>
                <div style="font-weight: 600;">${alert.symbol}</div>
                ${alert.companyName ? `<div style="font-size: 0.8rem; color: var(--text-secondary);">${alert.companyName}</div>` : ''}
            </td>
            <td>
                <span>${alert.condition}</span>
                <strong style="margin-left: 5px;">₹${alert.targetPrice}</strong>
            </td>
            <td>
                <span class="badge ${alert.triggered ? 'triggered' : 'pending'}">
                    ${alert.triggered ? 'Triggered' : 'Pending'}
                </span>
            </td>
            <td>
                <button class="icon-btn delete" onclick="deleteAlert(${alert.id})" title="Delete Alert">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                </button>
            </td>
        </tr>
    `).join('');
};

window.deleteAlert = async (id) => {
    if (!confirm('Are you sure you want to delete this alert?')) return;

    try {
        await apiFetch(`/api/alerts/${id}`, { method: 'DELETE' });
        showToast('Alert deleted', 'success');
        window.loadAlerts();
    } catch (e) {
        // ...
    }
};
