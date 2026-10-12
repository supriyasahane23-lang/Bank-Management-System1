/**
 * ApexBank - Frontend Core Application Logic
 */

const API_BASE = window.location.origin;

// -------------------------------------------------------------
// Toast Notification Utility
// -------------------------------------------------------------
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '❌';

    toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// -------------------------------------------------------------
// Modal Window Helpers
// -------------------------------------------------------------
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
    }
}

// Close modals when clicking overlay background
window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('active');
    }
});

// -------------------------------------------------------------
// Authentication & Customer Session
// -------------------------------------------------------------
function getCurrentUser() {
    const userStr = sessionStorage.getItem('bankUser');
    return userStr ? JSON.parse(userStr) : null;
}

function saveCurrentUser(user) {
    sessionStorage.setItem('bankUser', JSON.stringify(user));
}

function logoutCustomer() {
    sessionStorage.removeItem('bankUser');
    window.location.href = 'index.html';
}

// -------------------------------------------------------------
// Customer Login & Registration Handlers
// -------------------------------------------------------------
async function handleCustomerLogin(event) {
    event.preventDefault();
    const form = event.target;
    const identifier = form.identifier.value.trim();
    const pin = form.pin.value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ identifier, pin })
        });
        const result = await res.json();

        if (result.success) {
            saveCurrentUser(result.data);
            showToast('Welcome back, ' + result.data.fullName + '!', 'success');
            closeModal('loginModal');
            setTimeout(() => {
                window.location.href = 'dashboard.html';
            }, 800);
        } else {
            showToast(result.message || 'Login failed', 'error');
        }
    } catch (err) {
        showToast('Network error connecting to server.', 'error');
        console.error(err);
    }
}

async function handleCustomerRegister(event) {
    event.preventDefault();
    const form = event.target;
    const fullName = form.fullName.value.trim();
    const email = form.email.value.trim();
    const phone = form.phone.value.trim();
    const accountType = form.accountType.value;
    const initialDeposit = form.initialDeposit.value.trim() || '0';
    const pin = form.pin.value.trim();

    if (pin.length < 4) {
        showToast('PIN must be at least 4 digits!', 'error');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, email, phone, accountType, initialDeposit, pin })
        });
        const result = await res.json();

        if (result.success) {
            showToast(`Account #${result.data.accountNumber} created successfully!`, 'success');
            saveCurrentUser(result.data);
            closeModal('registerModal');
            setTimeout(() => {
                window.location.href = 'dashboard.html';
            }, 1200);
        } else {
            showToast(result.message || 'Registration failed', 'error');
        }
    } catch (err) {
        showToast('Network error during registration.', 'error');
        console.error(err);
    }
}

// Quick demo fill helper
function fillDemoUser(accNum, pin) {
    openModal('loginModal');
    const form = document.getElementById('loginForm');
    if (form) {
        form.identifier.value = accNum;
        form.pin.value = pin;
    }
}

// -------------------------------------------------------------
// Dashboard Actions
// -------------------------------------------------------------
let isBalanceHidden = false;

function toggleBalanceVisibility() {
    isBalanceHidden = !isBalanceHidden;
    const user = getCurrentUser();
    const balEl = document.getElementById('accountBalance');
    const eyeEl = document.getElementById('eyeIcon');
    if (balEl && user) {
        if (isBalanceHidden) {
            balEl.textContent = '••••••••';
            if (eyeEl) eyeEl.textContent = '👁️‍🗨️';
        } else {
            balEl.textContent = `$${parseFloat(user.balance).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
            if (eyeEl) eyeEl.textContent = '👁️';
        }
    }
}

async function refreshAccountData() {
    const user = getCurrentUser();
    if (!user) {
        window.location.href = 'index.html';
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/account?accountNumber=${encodeURIComponent(user.accountNumber)}`);
        const result = await res.json();
        if (result.success && result.data) {
            saveCurrentUser(result.data);
            renderDashboard(result.data);
            loadTransactions(result.data.accountNumber);
        } else {
            showToast('Failed to sync account info.', 'error');
        }
    } catch (e) {
        console.error('Error refreshing account:', e);
    }
}

function renderDashboard(user) {
    document.querySelectorAll('.user-name').forEach(el => el.textContent = user.fullName);
    document.querySelectorAll('.user-account').forEach(el => el.textContent = user.accountNumber);
    document.querySelectorAll('.user-type').forEach(el => el.textContent = user.accountType);
    
    const balEl = document.getElementById('accountBalance');
    if (balEl) {
        if (!isBalanceHidden) {
            balEl.textContent = `$${parseFloat(user.balance).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
        } else {
            balEl.textContent = '••••••••';
        }
    }

    const statusBadge = document.getElementById('userStatusBadge');
    if (statusBadge) {
        statusBadge.textContent = user.status;
        statusBadge.className = `badge badge-${user.status.toLowerCase()}`;
    }
}

async function loadTransactions(accountNumber) {
    const tbody = document.getElementById('transactionsTableBody');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE}/api/transactions?accountNumber=${encodeURIComponent(accountNumber)}&limit=50`);
        const result = await res.json();

        if (result.success && result.data) {
            const list = result.data;
            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color: var(--text-muted); padding: 2rem;">No transactions yet. Start by depositing or transferring funds!</td></tr>`;
                return;
            }

            let totalIn = 0;
            let totalOut = 0;

            tbody.innerHTML = list.map(tx => {
                const isPositive = (tx.type === 'DEPOSIT' || tx.type === 'TRANSFER_IN');
                const amt = parseFloat(tx.amount);
                if (isPositive) totalIn += amt;
                else totalOut += amt;

                const sign = isPositive ? '+' : '-';
                const amtClass = isPositive ? 'amount-pos' : 'amount-neg';
                const formattedDate = new Date(tx.createdAt).toLocaleString();

                return `
                    <tr>
                        <td>${formattedDate}</td>
                        <td>${escapeHtml(tx.description || '-')}</td>
                        <td><span class="badge badge-${tx.type.toLowerCase()}">${tx.type.replace('_', ' ')}</span></td>
                        <td class="${amtClass}">${sign}$${amt.toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                        <td>$${parseFloat(tx.balanceAfter).toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                    </tr>
                `;
            }).join('');

            const inEl = document.getElementById('totalMoneyIn');
            const outEl = document.getElementById('totalMoneyOut');
            if (inEl) inEl.textContent = `+$${totalIn.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
            if (outEl) outEl.textContent = `-$${totalOut.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
        }
    } catch (e) {
        console.error('Error fetching transactions:', e);
    }
}

// Handle Deposit
async function handleDeposit(event) {
    event.preventDefault();
    const user = getCurrentUser();
    if (!user) return;

    const form = event.target;
    const amount = form.amount.value.trim();
    const description = form.description.value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/deposit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountNumber: user.accountNumber, amount, description })
        });
        const result = await res.json();

        if (result.success) {
            showToast(result.message, 'success');
            form.reset();
            closeModal('depositModal');
            refreshAccountData();
        } else {
            showToast(result.message, 'error');
        }
    } catch (e) {
        showToast('Network error during deposit.', 'error');
    }
}

// Handle Withdraw
async function handleWithdraw(event) {
    event.preventDefault();
    const user = getCurrentUser();
    if (!user) return;

    const form = event.target;
    const amount = form.amount.value.trim();
    const pin = form.pin.value.trim();
    const description = form.description.value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/withdraw`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountNumber: user.accountNumber, amount, pin, description })
        });
        const result = await res.json();

        if (result.success) {
            showToast(result.message, 'success');
            form.reset();
            closeModal('withdrawModal');
            refreshAccountData();
        } else {
            showToast(result.message, 'error');
        }
    } catch (e) {
        showToast('Network error during withdrawal.', 'error');
    }
}

// Handle Transfer
async function handleTransfer(event) {
    event.preventDefault();
    const user = getCurrentUser();
    if (!user) return;

    const form = event.target;
    const toAccount = form.toAccount.value.trim();
    const amount = form.amount.value.trim();
    const pin = form.pin.value.trim();
    const description = form.description.value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/transfer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fromAccount: user.accountNumber, toAccount, amount, pin, description })
        });
        const result = await res.json();

        if (result.success) {
            showToast(result.message, 'success');
            form.reset();
            closeModal('transferModal');
            refreshAccountData();
        } else {
            showToast(result.message, 'error');
        }
    } catch (e) {
        showToast('Network error during transfer.', 'error');
    }
}

// Handle Change PIN
async function handleChangePin(event) {
    event.preventDefault();
    const user = getCurrentUser();
    if (!user) return;

    const form = event.target;
    const oldPin = form.oldPin.value.trim();
    const newPin = form.newPin.value.trim();
    const confirmPin = form.confirmPin.value.trim();

    if (newPin !== confirmPin) {
        showToast('New PIN and Confirm PIN do not match!', 'error');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/change-pin`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountNumber: user.accountNumber, oldPin, newPin })
        });
        const result = await res.json();

        if (result.success) {
            showToast('Security PIN successfully updated!', 'success');
            form.reset();
            closeModal('changePinModal');
        } else {
            showToast(result.message, 'error');
        }
    } catch (e) {
        showToast('Network error during PIN update.', 'error');
    }
}

function escapeHtml(str) {
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}
