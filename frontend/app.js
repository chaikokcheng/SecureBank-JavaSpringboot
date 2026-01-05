/* ============================================
   SecureBank - Frontend JavaScript
   ============================================ */

const API_URL = 'http://localhost:8080/api';
let authToken = localStorage.getItem('token');
let currentUser = JSON.parse(localStorage.getItem('user') || 'null');

// ============================================
// Initialization
// ============================================
document.addEventListener('DOMContentLoaded', () => {
    if (authToken && currentUser) {
        showDashboard();
    }
});

// ============================================
// Auth Functions
// ============================================
function showLogin() {
    document.getElementById('login-form').classList.remove('hidden');
    document.getElementById('register-form').classList.add('hidden');
}

function showRegister() {
    document.getElementById('login-form').classList.add('hidden');
    document.getElementById('register-form').classList.remove('hidden');
}

async function handleLogin(event) {
    event.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Login failed');
        }

        const data = await response.json();
        authToken = data.token;
        currentUser = data;
        
        localStorage.setItem('token', authToken);
        localStorage.setItem('user', JSON.stringify(currentUser));
        
        showToast('Welcome back, ' + data.firstName + '!', 'success');
        showDashboard();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function handleRegister(event) {
    event.preventDefault();
    const firstName = document.getElementById('register-firstname').value;
    const lastName = document.getElementById('register-lastname').value;
    const email = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;

    try {
        const response = await fetch(`${API_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ firstName, lastName, email, password })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Registration failed');
        }

        const data = await response.json();
        authToken = data.token;
        currentUser = data;
        
        localStorage.setItem('token', authToken);
        localStorage.setItem('user', JSON.stringify(currentUser));
        
        showToast('Account created! Welcome, ' + data.firstName + '!', 'success');
        showDashboard();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function logout() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    
    document.getElementById('auth-section').classList.remove('hidden');
    document.getElementById('dashboard-section').classList.add('hidden');
    
    // Clear forms
    document.getElementById('login-email').value = '';
    document.getElementById('login-password').value = '';
    
    showToast('Logged out successfully', 'info');
}

// ============================================
// Dashboard Functions
// ============================================
function showDashboard() {
    document.getElementById('auth-section').classList.add('hidden');
    document.getElementById('dashboard-section').classList.remove('hidden');
    
    document.getElementById('user-name').textContent = 
        currentUser.firstName + ' ' + currentUser.lastName;
    document.getElementById('account-number').textContent = 
        'Account: ' + currentUser.accountNumber;
    
    refreshBalance();
    loadHistory();
}

async function refreshBalance() {
    try {
        const response = await fetch(`${API_URL}/accounts/balance`, {
            headers: { 'Authorization': 'Bearer ' + authToken }
        });

        if (!response.ok) {
            if (response.status === 401) {
                logout();
                throw new Error('Session expired. Please login again.');
            }
            throw new Error('Failed to fetch balance');
        }

        const data = await response.json();
        document.getElementById('balance-amount').textContent = 
            '$' + parseFloat(data.balance).toFixed(2);
        document.getElementById('account-number').textContent = 
            'Account: ' + data.accountNumber;
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// ============================================
// Transfer Functions
// ============================================
async function handleTransfer(event) {
    event.preventDefault();
    
    const toAccountNumber = document.getElementById('transfer-to').value;
    const amount = parseFloat(document.getElementById('transfer-amount').value);
    const description = document.getElementById('transfer-description').value;

    try {
        const response = await fetch(`${API_URL}/transfer`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + authToken
            },
            body: JSON.stringify({ toAccountNumber, amount, description })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Transfer failed');
        }

        const data = await response.json();
        showToast('Transfer successful! $' + amount.toFixed(2) + ' sent.', 'success');
        
        // Clear form
        document.getElementById('transfer-to').value = '';
        document.getElementById('transfer-amount').value = '';
        document.getElementById('transfer-description').value = '';
        
        // Refresh data
        refreshBalance();
        loadHistory();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// ============================================
// Transaction History
// ============================================
async function loadHistory() {
    const listElement = document.getElementById('transaction-list');
    
    try {
        const response = await fetch(`${API_URL}/transfer/history`, {
            headers: { 'Authorization': 'Bearer ' + authToken }
        });

        if (!response.ok) {
            throw new Error('Failed to load history');
        }

        const transactions = await response.json();
        
        if (transactions.length === 0) {
            listElement.innerHTML = '<p class="empty-state">No transactions yet</p>';
            return;
        }

        listElement.innerHTML = transactions.map(tx => {
            const isSent = tx.type === 'SENT';
            const otherAccount = isSent ? tx.receiverAccountNumber : tx.senderAccountNumber;
            const date = new Date(tx.timestamp).toLocaleDateString('en-US', {
                month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
            });
            
            return `
                <div class="transaction-item">
                    <div class="transaction-info">
                        <div class="transaction-type ${isSent ? 'sent' : 'received'}">
                            ${isSent ? '↑ Sent' : '↓ Received'}
                        </div>
                        <div class="transaction-account">${isSent ? 'To: ' : 'From: '}${otherAccount}</div>
                        ${tx.description ? `<div class="transaction-description">${tx.description}</div>` : ''}
                    </div>
                    <div>
                        <div class="transaction-amount ${isSent ? 'sent' : 'received'}">
                            ${isSent ? '-' : '+'}$${parseFloat(tx.amount).toFixed(2)}
                        </div>
                        <div class="transaction-date">${date}</div>
                    </div>
                </div>
            `;
        }).join('');
    } catch (error) {
        listElement.innerHTML = '<p class="empty-state">Failed to load transactions</p>';
    }
}

// ============================================
// Toast Notifications
// ============================================
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toast-message');
    
    toast.className = 'toast ' + type;
    toastMessage.textContent = message;
    
    // Show toast
    setTimeout(() => toast.classList.add('show'), 10);
    
    // Hide after 3 seconds
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}
