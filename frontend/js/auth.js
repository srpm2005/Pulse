document.addEventListener('DOMContentLoaded', () => {
    // If we're on the login page and already have a token, check if it's valid, if so redirect
    if (window.location.pathname.endsWith('index.html') || window.location.pathname === '/') {
        if (getToken()) {
            apiFetch('/api/auth/me')
                .then(() => {
                    window.location.href = 'dashboard.html';
                })
                .catch(() => {
                    clearToken();
                });
        }
    }

    const authForm = document.getElementById('auth-form');
    if (!authForm) return;

    let isLoginMode = true;

    const toggleBtn = document.getElementById('toggle-btn');
    const toggleText = document.getElementById('toggle-text');
    const formTitle = document.getElementById('form-title');
    const formSubtitle = document.getElementById('form-subtitle');
    const submitBtn = document.getElementById('submit-btn');

    toggleBtn.addEventListener('click', () => {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            formTitle.textContent = 'Welcome back';
            formSubtitle.textContent = 'Enter your details to access your dashboard.';
            submitBtn.textContent = 'Sign In';
            toggleText.textContent = "Don't have an account? ";
            toggleBtn.textContent = 'Sign Up';
        } else {
            formTitle.textContent = 'Create an account';
            formSubtitle.textContent = 'Sign up to start tracking your portfolio.';
            submitBtn.textContent = 'Sign Up';
            toggleText.textContent = "Already have an account? ";
            toggleBtn.textContent = 'Sign In';
        }
    });

    authForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        submitBtn.disabled = true;
        submitBtn.textContent = 'Please wait...';

        const path = isLoginMode ? '/api/auth/login' : '/api/auth/register';

        try {
            const data = await apiFetch(path, {
                method: 'POST',
                body: JSON.stringify({ email, password })
            });

            if (data && data.token) {
                setToken(data.token);
                window.location.href = 'dashboard.html';
            }
        } catch (error) {
            // Error mapped in apiFetch and shown as Toast
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = isLoginMode ? 'Sign In' : 'Sign Up';
        }
    });
});

const logout = () => {
    clearToken();
    window.location.href = 'index.html';
};
