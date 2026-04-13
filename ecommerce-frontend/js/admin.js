const API_BASE = 'http://localhost:8080/api';

let currentUser = null;

// ─── Auth Guard ───────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    const u = localStorage.getItem('user');
    if (!u) { window.location.href = 'login.html'; return; }
    currentUser = JSON.parse(u);

    if (currentUser.role !== 'ADMIN' && currentUser.role !== 'INVENTORY_MANAGER') {
        document.body.innerHTML = `
            <div style="text-align:center; margin-top:8rem;">
                <h1 style="color:#ef4444;">403 – Forbidden</h1>
                <p>You don't have admin privileges.</p>
            </div>`;
        setTimeout(() => window.location.href = 'index.html', 3000);
        return;
    }

    loadAllOrders();
    loadMetrics();   // load financial dashboard on page arrival
});

document.getElementById('logout-btn').addEventListener('click', () => {
    localStorage.removeItem('user');
    window.location.href = 'login.html';
});

document.getElementById('refresh-orders-btn').addEventListener('click', loadAllOrders);

// ─── Helpers ─────────────────────────────────────────────────────────────────
const generalError   = document.getElementById('general-error');
const generalSuccess = document.getElementById('general-success');

function showError(msg) {
    generalError.textContent = 'Error: ' + msg;
    generalError.style.display = 'block';
    setTimeout(() => { generalError.style.display = 'none'; }, 6000);
}
function showSuccess(msg) {
    generalSuccess.textContent = msg;
    generalSuccess.style.display = 'block';
    setTimeout(() => { generalSuccess.style.display = 'none'; }, 4000);
}
function statusBadge(s) {
    return `<span class="status-badge status-${s}">${s}</span>`;
}

// Wrapper that attaches Admin-User-Id header to every request
async function adminFetch(url, options = {}) {
    if (!options.headers) options.headers = {};
    options.headers['Admin-User-Id'] = currentUser.userId;
    options.headers['Content-Type']  = 'application/json';

    const res = await fetch(url, options);
    const ct  = res.headers.get('content-type') || '';
    const data = ct.includes('application/json') ? await res.json() : await res.text();

    if (!res.ok) {
        const msg = (typeof data === 'object') ? (data.error || data.message || JSON.stringify(data)) : data;
        throw new Error(msg);
    }
    return data;
}

// ─── Add Product ─────────────────────────────────────────────────────────────
document.getElementById('add-product-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        name:  document.getElementById('prod-name').value.trim(),
        price: parseFloat(document.getElementById('prod-price').value)
    };
    try {
        const data = await adminFetch(`${API_BASE}/admin/products`, { method: 'POST', body: JSON.stringify(payload) });
        showSuccess(`Product created: ${data.name} (ID: ${data.productId})`);
        e.target.reset();
    } catch (err) { showError(err.message); }
});

// ─── Restock ─────────────────────────────────────────────────────────────────
document.getElementById('restock-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        productId:   parseInt(document.getElementById('restock-pid').value),
        warehouseId: parseInt(document.getElementById('restock-wid').value),
        quantityToAdd: parseInt(document.getElementById('restock-qty').value)
    };
    try {
        const data = await adminFetch(`${API_BASE}/admin/inventory/restock`, { method: 'PUT', body: JSON.stringify(payload) });
        showSuccess(data.message || 'Stock updated.');
        e.target.reset();
    } catch (err) { showError(err.message); }
});

// ─── Quick Dispatch buttons ───────────────────────────────────────────────────
document.getElementById('ship-btn').addEventListener('click', async () => {
    const oid = document.getElementById('dispatch-oid').value;
    if (!oid) { showError('Enter an Order ID'); return; }
    await dispatchOrder(oid, 'ship');
});
document.getElementById('deliver-btn').addEventListener('click', async () => {
    const oid = document.getElementById('dispatch-oid').value;
    if (!oid) { showError('Enter an Order ID'); return; }
    await dispatchOrder(oid, 'deliver');
});

// ─── Dispatch helper (reused by both table buttons & quick dispatch) ───────────
async function dispatchOrder(orderId, action) {
    try {
        const data = await adminFetch(`${API_BASE}/admin/orders/${orderId}/${action}`, { method: 'PUT' });
        showSuccess(data.message || `Order ${orderId} updated.`);
        loadAllOrders();
        loadMetrics();   // delivery losses may change after fulfillment
    } catch (err) { showError(err.message); }
}

// ─── Financial Impact Metrics ─────────────────────────────────────────────────
async function loadMetrics() {
    try {
        const data = await adminFetch(`${API_BASE}/admin/metrics/cancellations`);
        document.getElementById('metric-count').textContent        = data.totalCancelledOrders;
        document.getElementById('metric-delivery-loss').textContent = `$${data.totalDeliveryChargeLoss.toFixed(2)}`;
        document.getElementById('metric-fees').textContent          = `$${data.totalCancellationFeesCollected.toFixed(2)}`;
    } catch (err) {
        console.warn('Metrics load failed:', err.message);
    }
}

// ─── Fulfillment Table ────────────────────────────────────────────────────────
async function loadAllOrders() {
    const tbody = document.getElementById('fulfillment-body');
    tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted);">Loading…</td></tr>';
    try {
        const orders = await adminFetch(`${API_BASE}/admin/orders`);

        if (!orders.length) {
            tbody.innerHTML = '<tr><td colspan="6" style="color:var(--text-muted);">No orders in system.</td></tr>';
            return;
        }

        // Newest first
        orders.sort((a, b) => b.orderId - a.orderId);

        tbody.innerHTML = orders.map(o => {
            const items = (o.items || o.orderItems || []);
            const itemSummary = items.map(i => `${i.product?.name ?? '?'} ×${i.quantity}`).join(', ') || '—';
            const customerName = o.user?.name ?? `User #${o.user?.userId ?? '?'}`;

            let actions = '—';
            if (o.status === 'PAID') {
                actions = `<button class="btn-primary" style="padding:0.4rem 0.8rem;" onclick="dispatchOrder(${o.orderId},'ship')">Ship</button>`;
            } else if (o.status === 'SHIPPED') {
                actions = `<button class="btn-primary" style="padding:0.4rem 0.8rem;" onclick="dispatchOrder(${o.orderId},'deliver')">Deliver</button>`;
            }

            return `
            <tr id="fulfillment-row-${o.orderId}">
                <td><strong>#${o.orderId}</strong></td>
                <td>${customerName}</td>
                <td style="max-width:250px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;" title="${itemSummary}">${itemSummary}</td>
                <td>$${o.totalAmount.toFixed(2)}</td>
                <td>${statusBadge(o.status)}</td>
                <td>${actions}</td>
            </tr>`;
        }).join('');

    } catch (err) { showError(err.message); }
}

// Make dispatchOrder globally accessible from inline onclick
window.dispatchOrder = dispatchOrder;
