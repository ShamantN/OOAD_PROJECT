const API_BASE = 'http://localhost:8080/api';

// ─── Auth Guard ───────────────────────────────────────────────────────────────
let currentUser = null;
document.addEventListener('DOMContentLoaded', () => {
    const u = localStorage.getItem('user');
    if (!u) { window.location.href = 'login.html'; return; }
    currentUser = JSON.parse(u);
    document.getElementById('welcome-msg').textContent = `Welcome, ${currentUser.name}`;
    if (currentUser.role === 'ADMIN' || currentUser.role === 'INVENTORY_MANAGER') {
        document.getElementById('admin-link').style.display = 'inline-flex';
    }
    renderCartCount();
    loadCatalog();
    loadOrderHistory();
});

document.getElementById('logout-btn').addEventListener('click', () => {
    localStorage.removeItem('user');
    localStorage.removeItem('cart');
    window.location.href = 'login.html';
});

// ─── Helpers ─────────────────────────────────────────────────────────────────
const generalError   = document.getElementById('general-error');
const generalSuccess = document.getElementById('general-success');

function showError(msg) {
    generalError.textContent = msg;
    generalError.style.display = 'block';
    setTimeout(() => { generalError.style.display = 'none'; }, 5000);
}
function showSuccess(msg) {
    generalSuccess.textContent = msg;
    generalSuccess.style.display = 'block';
    setTimeout(() => { generalSuccess.style.display = 'none'; }, 4000);
}
function statusBadge(s) {
    return `<span class="status-badge status-${s}">${s}</span>`;
}
async function safeJson(res) {
    const ct = res.headers.get('content-type') || '';
    return ct.includes('application/json') ? res.json() : res.text();
}

// ─── Cart Logic ───────────────────────────────────────────────────────────────
function getCart() { return JSON.parse(localStorage.getItem('cart') || '[]'); }
function saveCart(c) { localStorage.setItem('cart', JSON.stringify(c)); }

function renderCartCount() {
    const cart = getCart();
    const total = cart.reduce((s, i) => s + i.quantity, 0);
    document.getElementById('cart-count').textContent = total;
}

window.addToCart = function(productId, name, price, maxStock) {
    const cart = getCart();
    const existing = cart.find(i => i.productId === productId);
    if (existing) {
        if (existing.quantity >= maxStock) { showError(`Max stock reached for ${name}.`); return; }
        existing.quantity++;
    } else {
        cart.push({ productId, name, price, quantity: 1 });
    }
    saveCart(cart);
    renderCartCount();
    showSuccess(`${name} added to cart!`);
};

function renderCartDrawer() {
    const cart = getCart();
    const drawer = document.getElementById('cart-drawer');
    const list   = document.getElementById('cart-items-list');
    const totalEl = document.getElementById('cart-total');

    if (cart.length === 0) { drawer.style.display = 'none'; return; }

    drawer.style.display = 'block';
    let grandTotal = 0;
    list.innerHTML = cart.map(item => {
        const sub = item.price * item.quantity;
        grandTotal += sub;
        return `
        <div class="order-row">
            <div>
                <strong>${item.name}</strong>
                <span style="color:var(--text-muted); margin-left:0.5rem;">× ${item.quantity}</span>
            </div>
            <div class="flex-gap" style="align-items:center;">
                <span style="color:var(--primary-color); font-weight:600;">$${sub.toFixed(2)}</span>
                <button class="btn-outline" onclick="changeQty(${item.productId}, -1)" style="padding:0.3rem 0.6rem;">−</button>
                <button class="btn-outline" onclick="changeQty(${item.productId}, 1)"  style="padding:0.3rem 0.6rem;">+</button>
                <button class="btn-danger"  onclick="removeFromCart(${item.productId})" style="padding:0.3rem 0.6rem;">✕</button>
            </div>
        </div>`;
    }).join('');
    totalEl.textContent = `$${grandTotal.toFixed(2)}`;
}

window.changeQty = function(productId, delta) {
    const cart = getCart();
    const idx  = cart.findIndex(i => i.productId === productId);
    if (idx === -1) return;
    cart[idx].quantity += delta;
    if (cart[idx].quantity <= 0) cart.splice(idx, 1);
    saveCart(cart);
    renderCartCount();
    renderCartDrawer();
};

window.removeFromCart = function(productId) {
    saveCart(getCart().filter(i => i.productId !== productId));
    renderCartCount();
    renderCartDrawer();
};

document.getElementById('cart-btn').addEventListener('click', renderCartDrawer);
document.getElementById('clear-cart-btn').addEventListener('click', () => {
    saveCart([]);
    renderCartCount();
    renderCartDrawer();
});

// ─── Checkout ────────────────────────────────────────────────────────────────
document.getElementById('checkout-btn').addEventListener('click', async () => {
    const cart = getCart();
    if (cart.length === 0) { showError('Your cart is empty!'); return; }

    // Calculate total from cart for the payment confirmation prompt
    let grandTotal = 0;
    cart.forEach(item => grandTotal += (item.price * item.quantity));
    const totalDisplay = grandTotal.toFixed(2);

    // Ask for payment upfront before finalizing checkout
    const proceedToPay = confirm(`Your checkout total is $${totalDisplay}.\n\nProceed to payment and place the order?`);
    if (!proceedToPay) {
        return; // User cancelled the payment
    }

    const payload = {
        user: { userId: currentUser.userId },
        items: cart.map(i => ({
            product: { productId: i.productId },
            quantity: i.quantity,
            price: i.price
        }))
    };

    try {
        // Step 1: Place the order to generate the Order ID
        const res = await fetch(`${API_BASE}/orders/place`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await safeJson(res);
        if (!res.ok) throw new Error(typeof data === 'object' ? (data.error || JSON.stringify(data)) : data);

        // Step 2: Automatically process the payment using the generated order ID
        const payRes = await fetch(`${API_BASE}/payments/${data.orderId}/pay`, { method: 'POST' });
        const payData = await safeJson(payRes);
        
        if (!payRes.ok) {
            // If payment fails but order was placed
            showError(`Order #${data.orderId} placed, but payment failed: ` + (typeof payData === 'object' ? payData.error : payData));
        } else {
            showSuccess(`✅ Order #${data.orderId} placed and paid! Total: $${data.totalAmount.toFixed(2)}`);
        }

        saveCart([]);
        renderCartCount();
        renderCartDrawer();
        loadCatalog();          // refresh stock numbers
        loadOrderHistory();     // show new order immediately
    } catch (err) {
        showError(err.message);
    }
});

// ─── Catalog ─────────────────────────────────────────────────────────────────
async function loadCatalog() {
    const grid = document.getElementById('product-grid');
    grid.innerHTML = '<p>Loading catalog…</p>';
    try {
        const res = await fetch(`${API_BASE}/catalog/products`);
        const products = await safeJson(res);
        if (!res.ok) throw new Error('Failed to load catalog');
        if (!products.length) { grid.innerHTML = '<p style="color:var(--text-muted)">No products in stock.</p>'; return; }

        grid.innerHTML = products.map(p => `
            <div class="card" id="product-card-${p.productId}">
                <h3>${p.name}</h3>
                <p class="product-price">$${p.price.toFixed(2)}</p>
                <p class="product-stock">In Stock: ${p.stock}</p>
                <button class="btn-secondary" style="width:100%;" onclick="addToCart(${p.productId}, '${p.name.replace(/'/g,"\\'")}', ${p.price}, ${p.stock})">
                    Add to Cart
                </button>
            </div>`).join('');
    } catch (err) { showError(err.message); }
}

// ─── Order History ────────────────────────────────────────────────────────────
async function loadOrderHistory() {
    const listEl = document.getElementById('orders-list');
    try {
        const res = await fetch(`${API_BASE}/orders/user/${currentUser.userId}`);
        const orders = await safeJson(res);
        if (!res.ok) throw new Error('Could not load orders');
        if (!orders.length) { listEl.innerHTML = '<p style="color:var(--text-muted)">No orders yet.</p>'; return; }

        // Sort newest first
        orders.sort((a, b) => b.orderId - a.orderId);

        listEl.innerHTML = orders.map(o => {
            const items = o.items || o.orderItems || [];
            const itemSummary = items.map(i => `${i.product?.name ?? 'Item'} × ${i.quantity}`).join(', ');
            return `
            <div class="order-row" id="order-row-${o.orderId}">
                <div>
                    <strong>Order #${o.orderId}</strong>
                    <span style="color:var(--text-muted); margin-left:0.75rem; font-size:0.875rem;">${itemSummary}</span>
                    <br><span style="color:var(--secondary-color); font-weight:600;">$${o.totalAmount.toFixed(2)}</span>
                </div>
                <div class="flex-gap" style="align-items:center;">
                    ${statusBadge(o.status)}
                    ${o.status === 'CREATED' ? `<button class="btn-primary" onclick="payOrder(${o.orderId}, ${o.totalAmount})">Pay Now</button>` : ''}
                    ${(o.status === 'CREATED' || o.status === 'PAID' || o.status === 'SHIPPED')
                        ? `<button class="btn-danger" style="padding:0.5rem 1rem;" onclick="cancelOrder(${o.orderId})">Cancel</button>`
                        : ''}
                </div>
            </div>`;
        }).join('');
    } catch (err) { showError(err.message); }
}

document.getElementById('refresh-orders-btn').addEventListener('click', loadOrderHistory);

// ─── Pay for Order ────────────────────────────────────────────────────────────
window.payOrder = async function(orderId) {
    try {
        const res = await fetch(`${API_BASE}/payments/${orderId}/pay`, { method: 'POST' });
        const data = await safeJson(res);
        if (!res.ok) throw new Error(typeof data === 'object' ? data.error : data);
        showSuccess(typeof data === 'string' ? data : 'Payment successful!');
        loadOrderHistory();
    } catch (err) { showError(err.message); }
};

// ─── Cancel Order ─────────────────────────────────────────────────────────────
window.cancelOrder = async function(orderId) {
    if (!confirm(`Cancel Order #${orderId}?\n\nNote: Orders in the SHIPPED state incur a 15% cancellation fee and a $10.00 delivery charge loss.\n\nContinue?`)) return;
    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}/cancel`, { method: 'POST' });
        const data = await safeJson(res);
        if (!res.ok) throw new Error(typeof data === 'object' ? (data.error || JSON.stringify(data)) : data);

        // data is a CancellationImpact object — build a detailed receipt alert
        const fee      = typeof data === 'object' ? data.cancellationFee        ?? 0 : 0;
        const loss     = typeof data === 'object' ? data.deliveryChargeLoss     ?? 0 : 0;
        const refund   = typeof data === 'object' ? data.finalRefund             ?? 0 : 0;
        const original = typeof data === 'object' ? data.refundableAmount        ?? 0 : 0;

        const receiptLines = [
            `✅ Order #${orderId} has been cancelled.`,
            ``,
            `──── Cancellation Receipt ────`,
            `Original Order Total:      $${original.toFixed(2)}`,
            fee > 0
                ? `Cancellation Fee (15%):  - $${fee.toFixed(2)}`
                : `Cancellation Fee:          $0.00  (No penalty)`,
            loss > 0
                ? `Delivery Charge Lost:    - $${loss.toFixed(2)}`
                : `Delivery Charge Lost:      $0.00`,
            `─────────────────────────────`,
            `Your Final Refund:         $${refund.toFixed(2)}`
        ].join('\n');

        alert(receiptLines);

        loadCatalog();        // stock restored — refresh counts
        loadOrderHistory();   // show CANCELLED badge
    } catch (err) {
        showError(err.message);
    }
};

// Exposed for automated testing
window.loadOrderHistory = loadOrderHistory;
