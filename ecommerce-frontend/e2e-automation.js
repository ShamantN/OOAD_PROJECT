/**
 * e2e-automation.js
 * ──────────────────────────────────────────────────────────────────────
 * Automated full-stack E2E test using Puppeteer.
 * Prerequisites:
 *   1. Spring Boot backend running on http://localhost:8080
 *   2. Frontend served on http://127.0.0.1:5500  (python -m http.server 5500)
 *   3. Fresh / clean database (no duplicate emails)
 *
 * Run:  node e2e-automation.js
 * ──────────────────────────────────────────────────────────────────────
 */

const puppeteer = require('puppeteer');

const FRONTEND = 'http://127.0.0.1:5500';
const TIMEOUT  = 8000;   // ms to wait for DOM updates

// ─── Assertion helper ─────────────────────────────────────────────────────────
function assert(condition, msg) {
    if (!condition) throw new Error(`❌ ASSERTION FAILED: ${msg}`);
    console.log(`   ✅ ${msg}`);
}

// ─── Unique run suffix so repeated runs don't clash on unique-email constraint ─
const RUN_ID    = Date.now();
const ADMIN_EMAIL    = `admin_${RUN_ID}@test.com`;
const CUSTOMER_EMAIL = `customer_${RUN_ID}@test.com`;
const PASSWORD  = 'testpass123';

(async () => {
    const browser = await puppeteer.launch({
        headless: false,          // set to true for CI / silent runs
        slowMo: 60,               // slows each action 60ms so you can watch
        args: ['--no-sandbox']
    });
    const page = await browser.newPage();
    page.setDefaultTimeout(TIMEOUT);

    // Capture console errors from the page to surface JS bugs
    page.on('console', msg => {
        if (msg.type() === 'error') console.warn(`  [PAGE ERROR] ${msg.text()}`);
    });

    // ─── Utility wrappers ────────────────────────────────────────────────────
    const go   = url         => page.goto(url, { waitUntil: 'networkidle0' });
    const type = (sel, val)  => page.type(sel, val);
    const click= sel         => page.click(sel);
    const wait = sel         => page.waitForSelector(sel, { visible: true });
    const text = async sel   => (await page.$(sel)) ? page.$eval(sel, el => el.textContent.trim()) : '';
    const select  = (sel, v) => page.select(sel, v);
    const clearLS = ()       => page.evaluate(() => localStorage.clear());

    // ─── 1. REGISTER CUSTOMER ─────────────────────────────────────────────────
    console.log('\n── Test 1: Customer Registration ────────────────────────────');
    await clearLS();
    await go(`${FRONTEND}/login.html`);
    await wait('#show-register');
    await click('#show-register');
    await wait('#register-section');

    await type('#reg-name',     'E2E Customer');
    await type('#reg-email',    CUSTOMER_EMAIL);
    await type('#reg-password', PASSWORD);
    await select('#reg-role',   'CUSTOMER');
    await click('#register-form button[type="submit"]');

    // Should redirect to index.html
    await page.waitForURL(`**/index.html`, { timeout: 5000 });
    const welcomeText = await text('#welcome-msg');
    assert(welcomeText.includes('E2E Customer'), `Welcome message shows customer name: "${welcomeText}"`);

    const stored = await page.evaluate(() => JSON.parse(localStorage.getItem('user') || 'null'));
    assert(stored !== null,            'user saved to localStorage');
    assert(stored.role === 'CUSTOMER', `role is CUSTOMER (got: ${stored.role})`);

    // ─── 2. REGISTER ADMIN ───────────────────────────────────────────────────
    console.log('\n── Test 2: Admin Registration ───────────────────────────────');
    await clearLS();
    await go(`${FRONTEND}/login.html`);
    await wait('#show-register');
    await click('#show-register');

    await type('#reg-name',     'E2E Admin');
    await type('#reg-email',    ADMIN_EMAIL);
    await type('#reg-password', PASSWORD);
    await select('#reg-role',   'ADMIN');
    await click('#register-form button[type="submit"]');

    await page.waitForURL(`**/admin.html`, { timeout: 5000 });
    const adminStored = await page.evaluate(() => JSON.parse(localStorage.getItem('user') || 'null'));
    assert(adminStored?.role === 'ADMIN', `Admin role stored correctly (got: ${adminStored?.role})`);

    // ─── 3. ADMIN CREATES PRODUCT & RESTOCKS ─────────────────────────────────
    console.log('\n── Test 3: Admin – Create Product & Restock ─────────────────');

    // Create product
    await wait('#prod-name');
    await type('#prod-name',  `E2E Phone ${RUN_ID}`);
    await type('#prod-price', '399');
    await click('#add-product-form button[type="submit"]');
    await wait('.success-message');
    const createMsg = await text('.success-message');
    assert(createMsg.toLowerCase().includes('product created'), `Product creation success: "${createMsg}"`);

    // Extract product ID from the success message (format: "Product created: E2E Phone … (ID: 7)")
    const idMatch = createMsg.match(/ID:\s*(\d+)/i);
    assert(idMatch, `Product ID found in success message`);
    const productId = parseInt(idMatch[1]);
    console.log(`   ℹ️  Product ID = ${productId}`);

    // Restock
    await page.evaluate(() => document.getElementById('restock-pid').value = '');
    await type('#restock-pid', String(productId));
    await type('#restock-qty', '50');
    await click('#restock-form button[type="submit"]');
    await page.waitForTimeout(1000);
    const restockMsg = await text('.success-message');
    assert(restockMsg.toLowerCase().includes('stock'), `Restock success: "${restockMsg}"`);

    // ─── 4. CUSTOMER ADDS TO CART & CHECKS OUT ───────────────────────────────
    console.log('\n── Test 4: Customer – Add to Cart & Checkout ────────────────');
    await clearLS();
    await go(`${FRONTEND}/login.html`);
    // Login as customer
    await wait('#login-email');
    await type('#login-email',    CUSTOMER_EMAIL);
    await type('#login-password', PASSWORD);
    await click('#login-form button[type="submit"]');
    await page.waitForURL(`**/index.html`, { timeout: 5000 });

    // Wait for catalog to load the product we just restocked
    await page.waitForSelector(`#product-card-${productId}`, { timeout: 6000 });

    // Add to cart
    await click(`#product-card-${productId} button`);
    await page.waitForTimeout(500);
    const cartCount = await text('#cart-count');
    assert(cartCount === '1', `Cart count is 1 (got: ${cartCount})`);

    // Open cart & checkout
    await click('#cart-btn');
    await wait('#cart-drawer');
    await click('#checkout-btn');

    // Wait for success badge and order list update
    await page.waitForSelector('.success-message', { visible: true });
    const checkoutMsg = await text('.success-message');
    assert(checkoutMsg.includes('Order'), `Checkout success message: "${checkoutMsg}"`);

    // Extract order ID from message
    const orderMatch = checkoutMsg.match(/Order #(\d+)/i);
    assert(orderMatch, 'Order ID found in success message');
    const orderId = parseInt(orderMatch[1]);
    console.log(`   ℹ️  Order ID = ${orderId}`);

    // ─── 5. ASSERT ORDER HISTORY SHOWS CREATED ───────────────────────────────
    console.log('\n── Test 5: Order History – CREATED state ────────────────────');
    await page.waitForSelector(`#order-row-${orderId}`, { timeout: 5000 });
    const rowText = await text(`#order-row-${orderId}`);
    assert(rowText.includes('CREATED'), `Order row shows status CREATED`);

    // ─── 6. PAY FOR THE ORDER ────────────────────────────────────────────────
    console.log('\n── Test 6: Customer – Pay for Order ─────────────────────────');
    await page.click(`#order-row-${orderId} button.btn-primary`);   // "Pay Now"
    await page.waitForTimeout(1500);
    await loadOrderHistoryAndWaitForStatus(page, orderId, 'PAID');
    console.log(`   ✅ Order #${orderId} status is PAID`);

    // ─── 7. ADMIN – SHIP THE ORDER ───────────────────────────────────────────
    console.log('\n── Test 7: Admin – Ship Order ───────────────────────────────');
    const adminUser = await page.evaluate((email) => {
        // We need admin creds – easiest: decode from a fresh login
        return fetch('http://localhost:8080/api/users/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password: arguments[1] })
        }).then(r => r.json());
    }, ADMIN_EMAIL, PASSWORD);

    await clearLS();
    await page.evaluate((u) => localStorage.setItem('user', JSON.stringify(u)), adminUser);
    await go(`${FRONTEND}/admin.html`);
    await wait('#fulfillment-body');
    await page.waitForSelector(`#fulfillment-row-${orderId}`, { timeout: 6000 });

    const rowInTable = await text(`#fulfillment-row-${orderId}`);
    assert(rowInTable.includes('PAID'), `Admin table shows order #${orderId} as PAID`);

    // Click Ship button in the table row
    await page.click(`#fulfillment-row-${orderId} button`);
    await page.waitForTimeout(1500);

    // Refresh and verify SHIPPED
    await click('#refresh-orders-btn');
    await page.waitForTimeout(1500);
    await page.waitForSelector(`#fulfillment-row-${orderId}`, { timeout: 5000 });
    const shippedRow = await text(`#fulfillment-row-${orderId}`);
    assert(shippedRow.includes('SHIPPED'), `Admin table shows order #${orderId} as SHIPPED`);

    // ─── 8. ADMIN – DELIVER THE ORDER (bonus) ────────────────────────────────
    console.log('\n── Test 8: Admin – Deliver Order ────────────────────────────');
    await page.click(`#fulfillment-row-${orderId} button`);
    await page.waitForTimeout(1500);
    await click('#refresh-orders-btn');
    await page.waitForTimeout(1500);
    await page.waitForSelector(`#fulfillment-row-${orderId}`);
    const deliveredRow = await text(`#fulfillment-row-${orderId}`);
    assert(deliveredRow.includes('DELIVERED'), `Admin table shows order #${orderId} as DELIVERED`);

    // ─── 9. SECURITY TEST – Customer cannot ship ─────────────────────────────
    console.log('\n── Test 9: Security – Customer spoofing Admin endpoint ───────');
    await clearLS();
    const custUser = JSON.parse(await page.evaluate(async (email, pwd) => {
        const r = await fetch('http://localhost:8080/api/users/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password: pwd })
        });
        return r.text();
    }, CUSTOMER_EMAIL, PASSWORD));
    await page.evaluate((u) => localStorage.setItem('user', JSON.stringify(u)), custUser);

    const forbiddenStatus = await page.evaluate(async (uid, oid) => {
        const r = await fetch(`http://localhost:8080/api/admin/orders/${oid}/ship`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'Admin-User-Id': uid }
        });
        return r.status;
    }, custUser.userId, orderId);

    assert(
        forbiddenStatus === 403 || forbiddenStatus === 400,
        `Backend blocks Customer from shipping (HTTP ${forbiddenStatus})`
    );

    // ─── All done ────────────────────────────────────────────────────────────
    console.log('\n══════════════════════════════════════════════════════════════');
    console.log('🎉  ALL TESTS PASSED — Full-stack E2E suite complete!');
    console.log('══════════════════════════════════════════════════════════════\n');

    await browser.close();
    process.exit(0);
})().catch(err => {
    console.error('\n🔴  TEST SUITE FAILED:', err.message);
    process.exit(1);
});

// ─── Poll helper: reload order history and wait for a specific status ─────────
async function loadOrderHistoryAndWaitForStatus(page, orderId, expectedStatus, retries = 6) {
    for (let i = 0; i < retries; i++) {
        await page.evaluate(async (userId) => {
            if (window.loadOrderHistory) await window.loadOrderHistory();
        });
        await page.waitForTimeout(800);

        const present = await page.evaluate((oid, status) => {
            const el = document.getElementById(`order-row-${oid}`);
            return el ? el.textContent.includes(status) : false;
        }, orderId, expectedStatus);

        if (present) return;
    }
    throw new Error(`Order #${orderId} never reached status ${expectedStatus} in the DOM`);
}
