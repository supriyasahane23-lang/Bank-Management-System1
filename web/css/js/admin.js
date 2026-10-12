/**
 * ApexBank - Admin Control Center Logic
 */

const API_BASE = window.location.origin;

function getAdminUser() {
    const str = sessionStorage.getItem('bankAdmin');
    return str ? JSON.parse(str) : null;
}

function saveAdminUser(admin) {
    sessionStorage.setItem('bankAdmin', JSON.stringify(admin));
}

function logoutAdmin() {
    sessionStorage.removeItem('bankAdmin');
    window.location.href = 'index.html';
}

// -------------------------------------------------------------
// Admin Authentication
// -------------------------------------------------------------
async function handleAdminLogin(event) {
    event.preventDefault();
    const form = event.target;
    const username = form.username.value.trim();
    const password = form.password.value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/admin/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const result = await res.json();

        if (result.success) {
            saveAdminUser(result.data);
            showToast('Welcome Administrator ' + result.data.fullName, 'success');
            setTimeout(() => {
                window.location.href = 'admin.html';
            }, 800);
        } else {
            showToast(result.message || 'Admin authentication failed', 'error');
        }
    } catch (e) {
        showToast('Network error during admin login.', 'error');
    }
}

// -------------------------------------------------------------
// Admin Dashboard Loader
// -------------------------------------------------------------
let allAccountsCache = [];

async function initAdminDashboard() {
    const admin = getAdminUser();
    if (!admin) {
        window.location.href = 'index.html';
        return;
    }

    const adminNameEl = document.getElementById('adminName');
    if (adminNameEl) adminNameEl.textContent = admin.fullName;

    await loadAdminStats();
    await loadAdminAccounts();
    await loadAdminTransactions();
}

async function loadAdminStats() {
    try {
        const res = await fetch(`${API_BASE}/api/admin/stats`);
        const result = await res.json();
        if (result.success && result.data) {
            const stats = result.data;
            document.getElementById('statTotalAccounts').textContent = stats.totalAccounts || 0;
            document.getElementById('statTotalReserve').textContent = `$${parseFloat(stats.totalReserve || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
            document.getElementById('statActiveAccounts').textContent = stats.activeAccounts || 0;
            document.getElementById('statTotalTransactions').textContent = stats.totalTransactions || 0;
        }
    } catch (e) {
        console.error('Error fetching admin stats:', e);
    }
}

async function loadAdminAccounts() {
    const tbody = document.getElementById('adminAccountsTableBody');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE}/api/admin/accounts`);
        const result = await res.json();
        if (result.success && result.data) {
            allAccountsCache = result.data;
            renderAdminAccountsTable(allAccountsCache);
        }
    } catch (e) {
        console.error('Error fetching admin accounts:', e);
    }
}

function renderAdminAccountsTable(accounts) {
    const tbody = document.getElementById('adminAccountsTableBody');
    if (!tbody) return;

    if (accounts.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color: var(--text-muted); padding: 2rem;">No accounts found.</td></tr>`;
        return;
    }

    tbody.innerHTML = accounts.map(acc => {
        const statusBadgeClass = `badge-${acc.status.toLowerCase()}`;
        const isFrozen = acc.status === 'FROZEN';

        return `
            <tr>
                <td><strong>#${acc.accountNumber}</strong></td>
                <td>${escapeHtml(acc.fullName)}</td>
                <td>${escapeHtml(acc.email)}<br><small style="color:var(--text-muted);">${escapeHtml(acc.phone)}</small></td>
                <td>${acc.accountType}</td>
                <td><strong>$${parseFloat(acc.balance).toLocaleString('en-US', { minimumFractionDigits: 2 })}</strong></td>
                <td><span class="badge ${statusBadgeClass}">${acc.status}</span></td>
                <td>
                    <div style="display:flex; gap:0.5rem;">
                        ${isFrozen 
                            ? `<button class="btn btn-success" style="padding:0.3rem 0.6rem; font-size:0.8rem;" onclick="updateAccountStatus('${acc.accountNumber}', 'ACTIVE')">Activate</button>`
                            : `<button class="btn btn-warning" style="padding:0.3rem 0.6rem; font-size:0.8rem;" onclick="updateAccountStatus('${acc.accountNumber}', 'FROZEN')">Freeze</button>`
                        }
                        <button class="btn btn-danger" style="padding:0.3rem 0.6rem; font-size:0.8rem;" onclick="updateAccountStatus('${acc.accountNumber}', 'CLOSED')">Close</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function filterAccounts() {
    const query = document.getElementById('searchAccountInput').value.toLowerCase().trim();
    if (!query) {
        renderAdminAccountsTable(allAccountsCache);
        return;
    }
    const filtered = allAccountsCache.filter(a => 
        a.accountNumber.toLowerCase().includes(query) ||
        a.fullName.toLowerCase().includes(query) ||
        a.email.toLowerCase().includes(query) ||
        a.phone.toLowerCase().includes(query)
    );
    renderAdminAccountsTable(filtered);
}

async function updateAccountStatus(accountNumber, newStatus) {
    if (!confirm(`Are you sure you want to change Account #${accountNumber} status to ${newStatus}?`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/admin/status`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountNumber, status: newStatus })
        });
        const result = await res.json();
        if (result.success) {
            showToast(result.message, 'success');
            await loadAdminStats();
            await loadAdminAccounts();
        } else {
            showToast(result.message, 'error');
        }
    } catch (e) {
        showToast('Error updating account status', 'error');
    }
}

async function loadAdminTransactions() {
    const tbody = document.getElementById('adminTransactionsTableBody');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE}/api/admin/transactions?limit=100`);
        const result = await res.json();
        if (result.success && result.data) {
            const list = result.data;
            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--text-muted); padding: 2rem;">No system transactions recorded yet.</td></tr>`;
                return;
            }

            tbody.innerHTML = list.map(tx => {
                const formattedDate = new Date(tx.createdAt).toLocaleString();
                const isPositive = (tx.type === 'DEPOSIT' || tx.type === 'TRANSFER_IN');
                const amtClass = isPositive ? 'amount-pos' : 'amount-neg';

                return `
                    <tr>
                        <td>${formattedDate}</td>
                        <td><strong>#${tx.accountNumber}</strong></td>
                        <td><span class="badge badge-${tx.type.toLowerCase()}">${tx.type.replace('_', ' ')}</span></td>
                        <td>${escapeHtml(tx.description || '-')}</td>
                        <td class="${amtClass}">$${parseFloat(tx.amount).toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                        <td>$${parseFloat(tx.balanceAfter).toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                    </tr>
                `;
            }).join('');
        }
    } catch (e) {
        console.error('Error fetching admin transactions:', e);
    }
}
