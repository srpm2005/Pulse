document.addEventListener('DOMContentLoaded', async () => {
    // Check auth
    if (!getToken()) {
        window.location.href = 'index.html';
        return;
    }

    try {
        const user = await apiFetch('/api/auth/me');
        document.getElementById('user-display').textContent = user.email;
    } catch (error) {
        // Will auto-redirect via apiFetch
        return;
    }

    // Navigation logic
    const navItems = document.querySelectorAll('.nav-item');
    const viewSections = document.querySelectorAll('.view-section');

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();

            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');

            const targetId = item.getAttribute('data-target');

            viewSections.forEach(section => {
                if (section.id === targetId) {
                    section.classList.remove('hidden');
                } else {
                    section.classList.add('hidden');
                }
            });

            if (targetId === 'alerts-view') {
                if (window.loadAlerts) window.loadAlerts();
            }
        });
    });

    // Modal global helpers
    window.openModal = (id) => {
        document.getElementById(id).classList.remove('hidden');
    };

    window.closeModal = (id) => {
        document.getElementById(id).classList.add('hidden');
    };

    // Initial bootstrap for stock widgets
    if (window.initStocks) {
        window.initStocks();
    }
});

const showLoader = () => document.getElementById('page-loader').classList.remove('hidden');
const hideLoader = () => document.getElementById('page-loader').classList.add('hidden');
