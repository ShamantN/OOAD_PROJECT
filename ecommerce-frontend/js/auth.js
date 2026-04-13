const API_BASE = 'http://localhost:8080/api';

// UI Elements
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const showRegisterLink = document.getElementById('show-register');
const showLoginLink = document.getElementById('show-login');

const loginSection = document.getElementById('login-section');
const registerSection = document.getElementById('register-section');

const loginError = document.getElementById('login-error');
const registerError = document.getElementById('register-error');

// Toggle Form Visibility
showRegisterLink.addEventListener('click', (e) => {
    e.preventDefault();
    loginSection.style.display = 'none';
    registerSection.style.display = 'block';
    loginError.style.display = 'none';
});

showLoginLink.addEventListener('click', (e) => {
    e.preventDefault();
    registerSection.style.display = 'none';
    loginSection.style.display = 'block';
    registerError.style.display = 'none';
});

// Registration Logic
registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    registerError.style.display = 'none';

    const payload = {
        name: document.getElementById('reg-name').value,
        email: document.getElementById('reg-email').value,
        password: document.getElementById('reg-password').value,
        role: document.getElementById('reg-role').value
    };

    try {
        const response = await fetch(`${API_BASE}/users/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const contentType = response.headers.get("content-type");
        let data;
        if (contentType && contentType.indexOf("application/json") !== -1) {
            data = await response.json();
        } else {
            data = await response.text();
        }

        if (!response.ok) {
            if (typeof data === 'object') {
                throw new Error(data.error || data.email || 'Registration failed');
            } else {
                throw new Error(data);
            }
        }

        // Auto login after registration
        handleAuthSuccess(data);

    } catch (err) {
        registerError.textContent = err.message;
        registerError.style.display = 'block';
    }
});

// Login Logic
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    loginError.style.display = 'none';

    const payload = {
        email: document.getElementById('login-email').value,
        password: document.getElementById('login-password').value
    };

    try {
        const response = await fetch(`${API_BASE}/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const contentType = response.headers.get("content-type");
        let data;
        if (contentType && contentType.indexOf("application/json") !== -1) {
            data = await response.json();
        } else {
            data = await response.text();
        }

        if (!response.ok) {
            if (typeof data === 'object') {
                throw new Error(data.error || 'Invalid credentials');
            } else {
                throw new Error(data);
            }
        }

        handleAuthSuccess(data);

    } catch (err) {
        loginError.textContent = err.message;
        loginError.style.display = 'block';
    }
});

// Post-auth routing
function handleAuthSuccess(user) {
    // Save to LocalStorage
    localStorage.setItem('user', JSON.stringify(user));
    
    // Redirect based on Role
    if (user.role === 'ADMIN' || user.role === 'INVENTORY_MANAGER') {
        window.location.href = 'admin.html';
    } else {
        window.location.href = 'index.html';
    }
}

// Initialization Check - if already logged in, redirect away from login
document.addEventListener('DOMContentLoaded', () => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (user) {
        handleAuthSuccess(user);
    }
});
