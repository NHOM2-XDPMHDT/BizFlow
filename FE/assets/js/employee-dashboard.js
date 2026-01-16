const API_BASE = resolveApiBase();

let products = [];
let cart = [];
let selectedCustomer = null;
let selectedEmployee = null;
let currentCategory = 'all';
let topSearchTerm = '';
let bottomSearchTerm = '';
let currentSort = 'name';
let customersLoaded = false;
let customers = [];
let customerSearchTerm = '';
let currentPaymentMethod = 'CASH';
let invoices = [];
let savedInvoices = [];
let activeInvoiceId = null;
let invoiceSequence = 1;
let cityCache = [];
let districtCache = [];
let wardCache = [];
let employeesLoaded = false;
let employees = [];
let exchangeDraft = null;

const PRODUCT_ICON = `
    <svg viewBox="0 0 24 24" class="icon-svg" aria-hidden="true">
        <path d="M6 8h12l-1.2 11H7.2L6 8Z" />
        <path d="M9 8V6a3 3 0 0 1 6 0v2" />
    </svg>
`;

window.addEventListener('DOMContentLoaded', async () => {
    checkAuth();
    loadUserInfo();
    await loadCurrentEmployee();
    selectedCustomer = { id: 0, name: 'Khách lẻ', phone: '-' };
    const selectedCustomerLabel = document.getElementById('selectedCustomer');
    if (selectedCustomerLabel) {
        selectedCustomerLabel.textContent = 'Khách lẻ';
    }
    setupEventListeners();
    setupCustomerModal();
    setupProductDetailModal();
    setupCustomerDetailModal();
    setupEmployeeSelector();
    setupAppMenuModal();
    setupInvoiceModal();
    toggleCashPanel(true);
    initInvoices();

    document.getElementById('productsGrid').innerHTML =
        '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #999;">Đang tải...</div>';

    await Promise.all([loadProducts()]);
    applyExchangeDraft();

    const customerSearchInput = document.getElementById('customerSearch');
    customerSearchInput?.addEventListener('focus', () => {
        if (customerSearchInput.classList.contains('has-selection')) {
            return;
        }
        const customerList = document.getElementById('customerList');
        if (customerList) {
            customerList.style.display = 'block';
        }
        if (!customersLoaded) {
            loadCustomers().then(() => applyCustomerFilter());
            return;
        }
        applyCustomerFilter();
    });
});

function applyExchangeDraft() {
    const raw = sessionStorage.getItem('exchangeDraft');
    if (!raw) return;
    try {
        exchangeDraft = JSON.parse(raw);
    } catch (err) {
        console.error('Invalid exchange draft', err);
        sessionStorage.removeItem('exchangeDraft');
        exchangeDraft = null;
        return;
    }
    if (!exchangeDraft || !Array.isArray(exchangeDraft.items) || exchangeDraft.items.length === 0) {
        return;
    }

    cart = exchangeDraft.items.map(item => ({
        productId: item.productId,
        productName: item.productName,
        productPrice: Number(item.price) || 0,
        quantity: -Math.abs(Number(item.quantity) || 0),
        productCode: item.productCode || '',
        unit: item.unit || '',
        stock: 1,
        isReturnItem: true
    })).filter(item => item.quantity !== 0);

    if (exchangeDraft.customerId) {
        selectedCustomer = {
            id: exchangeDraft.customerId,
            name: exchangeDraft.customerName || 'Khách hàng',
            phone: exchangeDraft.customerPhone || '-'
        };
        const searchInput = document.getElementById('customerSearch');
        if (searchInput) {
            searchInput.value = selectedCustomer.name || selectedCustomer.phone || '';
            searchInput.classList.add('has-selection');
            searchInput.readOnly = true;
        }
        const addBtn = document.getElementById('addCustomerBtn');
        if (addBtn) {
            addBtn.style.display = 'none';
        }
        const clearBtn = document.getElementById('clearCustomerBtn');
        if (clearBtn) {
            clearBtn.style.display = 'inline-flex';
        }
    }

    renderCart();
    updateTotal();
    sessionStorage.removeItem('exchangeDraft');
}

function resolveApiBase() {
    const configured = window.API_BASE_URL || window.API_BASE;
    if (configured) {
        return configured.replace(/\/$/, '');
    }

    if (window.location.protocol === 'file:') {
        return 'http://localhost:8080/api';
    }

    return `${window.location.origin}/api`;
}

function setupAppMenuModal() {
    const modal = document.getElementById('appMenuModal');
    const openBtn = document.getElementById('appMenuBtn');
    const closeBtn = document.getElementById('closeAppMenu');
    if (!modal || !openBtn || !closeBtn) return;
    const menuGrid = modal.querySelector('.app-menu-grid');

    openBtn.addEventListener('click', () => {
        modal.classList.add('show');
        modal.setAttribute('aria-hidden', 'false');
    });

    closeBtn.addEventListener('click', () => {
        modal.classList.remove('show');
        modal.setAttribute('aria-hidden', 'true');
    });

    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.remove('show');
            modal.setAttribute('aria-hidden', 'true');
        }
    });

    menuGrid?.addEventListener('click', (e) => {
        const tile = e.target.closest('.app-tile[data-app]');
        if (!tile) return;
        const target = tile.dataset.app;
        const route = resolveAppRoute(target);
        if (route) {
            window.location.href = route;
        }
    });
}

function resolveAppRoute(target) {
    switch (target) {
        case 'pos':
            return '/pages/employee-dashboard.html';
        case 'promotions':
            return '/pages/promotions.html';
        case 'orders':
            return '/pages/order-list.html';
        case 'invoices':
            return '/pages/invoice-list.html';
        case 'online-orders':
            return '/pages/online-orders.html';
        case 'returns':
            return '/pages/return-orders.html';
        case 'transfers':
            return '/pages/transfer-requests.html';
        case 'topup':
            return '/pages/topup-wallet.html';
        case 'cashflow':
            return '/pages/cashflow.html';
        case 'secondary':
            return '/pages/secondary-screen.html';
        case 'print':
            return '/pages/print-templates.html';
        case 'daily-report':
            return '/pages/daily-report.html';
        case 'access-log':
            return '/pages/access-log.html';
        case 'management':
            return '/pages/management.html';
        case 'display':
            return '/pages/display-settings.html';
        case 'einvoice':
            return '/pages/einvoice-mtt.html';
        case 'guide':
            return '/pages/guide.html';
        case 'feedback':
            return '/pages/feedback.html';
        case 'intro':
            return '/pages/introduction.html';
        default:
            return '';
    }
}

function checkAuth() {
    const token = sessionStorage.getItem('accessToken');
    const role = sessionStorage.getItem('role');

    if (!token || role !== 'EMPLOYEE') {
        window.location.href = '/pages/login.html';
    }
}

function loadUserInfo() {
    const username = sessionStorage.getItem('username');
    const userInitial = (username ? username[0] : 'E').toUpperCase();

    document.getElementById('userInitial').textContent = userInitial;
    document.getElementById('userNameDropdown').textContent = username || 'Nhân viên';
}

async function loadCurrentEmployee() {
    const userId = sessionStorage.getItem('userId');
    if (!userId) return;

    try {
        const res = await fetch(`${API_BASE}/users/${userId}`, {
            headers: {
                'Authorization': `Bearer ${sessionStorage.getItem('accessToken') || ''}`
            }
        });

        if (!res.ok) {
            if (res.status === 401) {
                sessionStorage.clear();
                window.location.href = '/pages/login.html';
                return;
            }
            throw new Error('Không tải được thông tin nhân viên');
        }

        const data = await res.json();
        selectedEmployee = {
            id: data.id,
            username: data.username,
            name: data.fullName || data.username
        };

        const employeeNameEl = document.getElementById('selectedEmployeeName');
        if (employeeNameEl) {
            employeeNameEl.textContent = selectedEmployee.name || selectedEmployee.username;
        }

        const userNameDropdown = document.getElementById('userNameDropdown');
        if (userNameDropdown) {
            userNameDropdown.textContent = data.username || data.fullName || 'Nhân viên';
        }

        const userInitialEl = document.getElementById('userInitial');
        if (userInitialEl) {
            const initial = (data.fullName || data.username || 'E')[0]?.toUpperCase() || 'E';
            userInitialEl.textContent = initial;
        }
    } catch (err) {
    }
}

async function loadProducts() {
    try {
        const response = await fetch(`${API_BASE}/products`, {
            headers: { 'Authorization': `Bearer ${sessionStorage.getItem('accessToken')}` }
        });

        if (!response.ok) {
            if (response.status === 401) {
                sessionStorage.clear();
                window.location.href = '/pages/login.html';
                return;
            }
            throw new Error('Failed to load products');
        }

        products = await response.json();
        filterProducts();
    } catch (err) {
        renderProducts([]);
    }
}

async function loadCustomers() {
    if (customersLoaded) return;

    try {
        const response = await fetch(`${API_BASE}/customers`, {
            headers: { 'Authorization': `Bearer ${sessionStorage.getItem('accessToken')}` }
        });

        if (!response.ok) {
            if (response.status === 401) {
                sessionStorage.clear();
                window.location.href = '/pages/login.html';
                return;
            }
            throw new Error('Failed to load customers');
        }

        const rawCustomers = await response.json();
        customers = dedupeCustomers(rawCustomers);
        applyCustomerFilter();
        customersLoaded = true;
    } catch (err) {
    }
}

function renderCustomers(customers) {
    const list = document.getElementById('customerList');
    if (!list) return;

    if (!customers || customers.length === 0) {
        const emptyMessage = customerSearchTerm ? 'Không tìm thấy khách hàng' : 'Chưa có khách hàng';
        list.innerHTML = `<div class="customer-empty">${emptyMessage}</div>`;
        return;
    }

    const customersHtml = customers.map(c => {
        const phone = c.phone || '-';
        const secondary = c.email || c.address || '-';
        const customerId = c.id ?? '';
        return `
        <div class="customer-item"
            data-customer-id="${customerId}"
            data-customer-name="${escapeHtml(c.name || '')}"
            data-customer-phone="${escapeHtml(phone)}">
            <div class="customer-info">
                <p class="customer-name">
                    <button type="button" class="customer-name-btn" data-customer-id="${customerId}" onclick="openCustomerDetailFromButton(event)">
                        ${escapeHtml(c.name || 'Khách hàng')}
                    </button>
                </p>
                <p class="customer-phone">${escapeHtml(phone)}</p>
            </div>
            <div class="customer-meta">
                <span class="customer-phone">${escapeHtml(phone)}</span>
                <span class="customer-sub">${escapeHtml(secondary)}</span>
            </div>
        </div>
        `;
    }).join('');

    list.innerHTML = customersHtml || '<div class="customer-empty">Chưa có khách hàng</div>';
}

function applyCustomerFilter() {
    const keyword = customerSearchTerm.trim().toLowerCase();
    const searchInput = document.getElementById('customerSearch');
    const list = document.getElementById('customerList');
    if (searchInput?.classList.contains('has-selection')) {
        if (list) {
            list.style.display = 'none';
        }
        return;
    }
    if (list) {
        list.style.display = 'block';
    }
    if (!keyword) {
        renderCustomers(getSuggestedCustomers());
        return;
    }

    const filtered = customers.filter(c => {
        const name = (c.name || '').toLowerCase();
        const phone = (c.phone || '').toLowerCase();
        const email = (c.email || '').toLowerCase();
        return name.includes(keyword) || phone.includes(keyword) || email.includes(keyword);
    });

    renderCustomers(filtered);
}

function getSuggestedCustomers() {
    return customers.slice(0, 8);
}

function normalizeCustomerKey(customer) {
    const phone = (customer.phone || '').replace(/\s+/g, '');
    if (phone) return `phone:${phone}`;
    return `id:${customer.id || Math.random()}`;
}

function dedupeCustomers(list) {
    const seen = new Map();
    (list || []).forEach(c => {
        const key = normalizeCustomerKey(c);
        if (!seen.has(key)) {
            seen.set(key, c);
        }
    });
    return Array.from(seen.values());
}

function addToCart(productId, productName, productPrice) {
    const qty = getCurrentQty();
    const splitLine = document.getElementById('splitLine')?.checked;
    const product = products.find(p => p.id === productId) || {};
    const stock = getStockValue(product);
    const effectivePrice = getEffectivePrice(product);
    const resolvedPrice = Number.isFinite(effectivePrice) ? effectivePrice : (productPrice || 0);

    if (!splitLine) {
        const existingItem = cart.find(item => item.productId === productId);
        if (existingItem) {
            existingItem.quantity += qty;
            existingItem.stock = stock;
            existingItem.productPrice = resolvedPrice;
        } else {
            cart.push({
                productId,
                productName,
                productPrice: resolvedPrice,
                quantity: qty,
                productCode: product.code || product.barcode || '',
                unit: product.unit || '',
                stock
            });
        }
    } else {
        cart.push({
            productId,
            productName,
            productPrice: resolvedPrice,
            quantity: qty,
            productCode: product.code || product.barcode || '',
            unit: product.unit || '',
            stock
        });
    }

    renderCart();
    updateTotal();
}

function clearCart(resetCustomer = true) {
    cart = [];
    renderCart();
    updateTotal();
    if (resetCustomer) {
        clearSelectedCustomer();
    }
}

async function createOrder(isPaid) {
    if (cart.length === 0) {
        showPopup('Giỏ hàng trống!', { type: 'error' });
        return;
    }
    const outOfStock = cart.find(item => !item.isReturnItem && (!Number.isFinite(Number(item.stock)) || Number(item.stock) <= 0));
    if (outOfStock) {
        showPopup('Có sản phẩm hết hàng. Vui lòng kiểm tra số lượng tồn.', { type: 'error' });
        return;
    }

    const userId = parseInt(sessionStorage.getItem('userId'), 10) || null;
    const customerId = selectedCustomer && selectedCustomer.id > 0 ? selectedCustomer.id : null;
    const payload = {
        userId,
        customerId,
        paid: isPaid,
        // Always send the chosen payment method so backend can create pending transfer payments with tokens
        paymentMethod: currentPaymentMethod,
        orderType: exchangeDraft ? 'EXCHANGE' : null,
        originalOrderId: exchangeDraft?.originalOrderId || null,
        items: cart.map(item => ({
            productId: item.productId,
            quantity: item.quantity
        }))
    };

    try {
        const res = await fetch(`${API_BASE}/orders`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${sessionStorage.getItem('accessToken') || ''}`
            },
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const message = await res.text();
            showPopup(message || 'Không thể tạo đơn hàng.', { type: 'error' });
            return;
        }

        const data = await res.json();
        const receiptData = buildReceiptData(data);
        const invoiceCode = receiptData.invoiceNumber || '-';
        showPopup(
            isPaid ? `Thanh toán thành công. Mã hóa đơn: ${invoiceCode}` : `Đã lưu tạm đơn: ${invoiceCode}`,
            { type: 'success' }
        );
        if (isPaid) {
            openInvoiceModal(receiptData);
            const shouldPrint = document.getElementById('printInvoiceToggle')?.checked !== false;
            if (shouldPrint) {
                setTimeout(() => window.print(), 150);
            }
        }
        clearCart(true);
        if (exchangeDraft) {
            sessionStorage.removeItem('exchangeDraft');
            exchangeDraft = null;
        }
        saveActiveInvoiceState();
        return data;
    } catch (err) {
        showPopup('Lỗi kết nối khi tạo đơn hàng.', { type: 'error' });
    }
}

function showTransferQrModal(orderId, amount, token) {
    const modal = document.getElementById('transferQrModal');
    if (!modal) return;
    modal.classList.add('show');
    modal.setAttribute('aria-hidden', 'false');
    document.getElementById('transferOrderId').textContent = orderId;
    document.getElementById('transferOrderIdSmall').textContent = orderId;
    document.getElementById('transferAmount').textContent = formatPrice(amount);

    // Ensure token is present (backend token preferred)
    let displayToken = token;
    if (displayToken) {
        const tokenEl = document.getElementById('transferPaymentToken');
        if (tokenEl) tokenEl.textContent = displayToken;
    } else {
        displayToken = 'SAMPLE-' + Math.random().toString(36).slice(2, 10).toUpperCase();
        const tokenEl = document.getElementById('transferPaymentToken');
        if (tokenEl) tokenEl.textContent = displayToken + ' (mã tượng trưng)';
    }

    const bankCode = 'VCB';
    const account = '1021209511';
    const accountName = 'BIZFLOW CO';

    const payloadEl = document.getElementById('transferPayload');
    if (payloadEl) {
        payloadEl.textContent = `VietQR • ${bankCode} • ${account} • ${accountName} • ${formatPrice(amount)}`;
    }

    const bankLogos = {
        VCB: 'https://img.vietqr.io/image/vietcombank-1021209511-compact.jpg'
    };
    const logoUrl = bankLogos[bankCode];
    const logoEl = document.getElementById('transferBankLogo');
    if (logoEl) {
        if (logoUrl) {
            logoEl.src = logoUrl;
            logoEl.style.display = '';
        } else {
            logoEl.style.display = 'none';
        }
    }

    const qrContainer = document.getElementById('qrCodeContainer');

    const bankQuickId = 'VCB';
    const template = 'compact';
    const amountParam = Number.isFinite(Number(amount)) && amount > 0 ? `amount=${Math.round(amount)}` : '';
    const addInfoParam = `addInfo=${encodeURIComponent('Thanh toán đơn #' + orderId)}`;
    const accountNameParam = `accountName=${encodeURIComponent(accountName)}`;
    const qrQuicklinkBase = `https://img.vietqr.io/image/${bankQuickId}-${account}-${template}.png`;
    const qrImgUrl = qrQuicklinkBase + (amountParam || addInfoParam || accountNameParam ? `?${[amountParam, addInfoParam, accountNameParam].filter(Boolean).join('&')}` : '');

    if (qrContainer) {
        qrContainer.innerHTML = '';
        const img = document.createElement('img');
        img.alt = 'VietQR';
        img.width = 260;
        img.height = 260;
        img.style.maxWidth = '100%';
        img.style.borderRadius = '8px';
        img.src = qrImgUrl;

        img.onload = () => {
            qrContainer.innerHTML = '';
            qrContainer.appendChild(img);
        };

        img.onerror = async () => {
            qrContainer.innerHTML = '';
            try {
                const fallbackPayload = `VietQR|BANK:${bankCode}|ACC:${account}|NAME:${accountName}|AMOUNT:${formatPriceCompact(amount)}|ORDER:${orderId}|TOKEN:${displayToken}`;
                if (window.QRCode) {
                    new QRCode(qrContainer, { text: fallbackPayload, width: 260, height: 260 });
                    const inner = qrContainer.querySelector('img,canvas');
                    if (inner) inner.style.borderRadius = '8px';
                } else {
                    qrContainer.textContent = `Mã: ${displayToken}`;
                }
            } catch (e) {
                qrContainer.textContent = `Mã: ${displayToken}`;
            }
        };

        qrContainer.appendChild(img);
    }

    const downloadBtn = document.getElementById('downloadQrBtn');
    if (downloadBtn) {
        downloadBtn.onclick = async () => {
            try {
                const res = await fetch(qrImgUrl);
                if (!res.ok) throw new Error('Failed to download QR image');
                const blob = await res.blob();
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `vietqr-order-${orderId}.png`;
                document.body.appendChild(a);
                a.click();
                a.remove();
                URL.revokeObjectURL(url);
            } catch (e) {
                alert('Không thể tải mã QR.');
            }
        };
    }

    // Copy token handler
    const copyBtn = document.getElementById('copyTokenBtn');
    if (copyBtn) {
        copyBtn.onclick = async () => {
            try {
                await navigator.clipboard.writeText(displayToken);
                alert('Đã sao chép mã thanh toán');
            } catch (e) {
                alert('Không thể sao chép mã.');
            }
        };
    }
}

// Duplicate showTransferQrModal removed — using the enhanced implementation above.

async function payOrder(orderId) {
    try {
        const token = document.getElementById('transferPaymentToken')?.textContent || null;
        const body = { method: 'TRANSFER' };
        if (token) body.token = token;

        const res = await fetch(`${API_BASE}/orders/${orderId}/pay`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${sessionStorage.getItem('accessToken') || ''}`
            },
            body: JSON.stringify(body)
        });
        if (!res.ok) {
            const text = await res.text();
            alert(text || 'Không thể xác nhận thanh toán.');
            return;
        }
        alert('✅ Thanh toán chuyển khoản được xác nhận.');
        hideTransferQrModal();
        clearCart(true);
        saveActiveInvoiceState();
    } catch (err) {
        alert('Lỗi kết nối khi xác nhận thanh toán.');
    }
}

function hideTransferQrModal() {
    const modal = document.getElementById('transferQrModal');
    if (!modal) return;
    modal.classList.remove('show');
    modal.setAttribute('aria-hidden', 'true');
}

// Duplicate payOrder removed — using the token-aware implementation above.

// wire modal buttons
document.addEventListener('DOMContentLoaded', () => {
    const confirmBtn = document.getElementById('transferConfirmBtn');
    const closeBtn = document.getElementById('transferCloseBtn');
    const cancelBtn = document.getElementById('transferCancelBtn');
    if (confirmBtn) confirmBtn.addEventListener('click', async () => {
        const orderId = document.getElementById('transferOrderId').textContent;
        await payOrder(orderId);
    });
    if (closeBtn) closeBtn.addEventListener('click', hideTransferQrModal);
    if (cancelBtn) cancelBtn.addEventListener('click', hideTransferQrModal);
});

function setupInvoiceModal() {
    const modal = document.getElementById('invoiceModal');
    const closeBtn = document.getElementById('closeInvoiceModal');
    const footerCloseBtn = document.getElementById('closeInvoiceBtn');
    const printBtn = document.getElementById('printInvoiceBtn');
    if (!modal) return;

    const close = () => closeInvoiceModal();
    closeBtn?.addEventListener('click', close);
    footerCloseBtn?.addEventListener('click', close);
    printBtn?.addEventListener('click', () => window.print());
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            close();
        }
    });
}

function openInvoiceModal(receiptData) {
    const modal = document.getElementById('invoiceModal');
    if (!modal || !receiptData) return;

    const itemsEl = document.getElementById('invoiceItems');
    if (itemsEl) {
        itemsEl.innerHTML = receiptData.items.map(item => `
            <div class="receipt-item">
                <span class="name">${escapeHtml(item.name)}</span>
                <span>${item.quantity}</span>
                <span>${formatPrice(item.price)}</span>
                <span>${formatPrice(item.total)}</span>
            </div>
        `).join('');
    }

    setText('invoiceCode', receiptData.invoiceNumber || '-');
    setText('invoiceDate', formatDateTime(receiptData.createdAt));
    setText('invoiceCashier', receiptData.cashier || '-');
    setText('invoiceCustomer', receiptData.customerName || 'Khách lẻ');
    setText('invoicePhone', receiptData.customerPhone || '-');
    setText('invoiceMethod', mapPaymentMethod(receiptData.paymentMethod));
    setText('invoiceSubtotal', formatPrice(receiptData.subtotal || 0));
    setText('invoiceTotal', formatPrice(receiptData.total || 0));
    setText('invoiceCashReceived', formatPrice(receiptData.cashReceived || 0));
    setText('invoiceChange', formatPrice(receiptData.change || 0));

    const noteWrap = document.getElementById('invoiceNoteWrap');
    const noteValue = receiptData.note || '';
    setText('invoiceNote', noteValue || '-');
    if (noteWrap) {
        noteWrap.style.display = noteValue ? 'grid' : 'none';
    }

    modal.classList.add('show');
    modal.setAttribute('aria-hidden', 'false');
}

function closeInvoiceModal() {
    const modal = document.getElementById('invoiceModal');
    if (!modal) return;
    modal.classList.remove('show');
    modal.setAttribute('aria-hidden', 'true');
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) {
        el.textContent = value;
    }
}

function buildReceiptData(orderResponse) {
    const subtotal = cart.reduce((sum, item) => sum + (item.productPrice * item.quantity), 0);
    const total = Math.max(0, subtotal);
    const cashReceived = parseInt(document.getElementById('cashReceivedInput')?.value, 10) || 0;
    const change = currentPaymentMethod === 'CASH' ? Math.max(0, cashReceived - total) : 0;

    let invoiceNumber = orderResponse?.invoiceNumber || '';
    if (!invoiceNumber && orderResponse?.orderId) {
        invoiceNumber = `HD-${orderResponse.orderId}`;
    }
    if (!invoiceNumber) {
        invoiceNumber = '-';
    }

    return {
        invoiceNumber,
        createdAt: new Date(),
        customerName: selectedCustomer?.name || 'Khách lẻ',
        customerPhone: selectedCustomer?.phone || '-',
        cashier: selectedEmployee?.name || sessionStorage.getItem('username') || 'Nhân viên',
        paymentMethod: currentPaymentMethod,
        note: document.getElementById('paymentNote')?.value?.trim() || '',
        subtotal,
        total,
        cashReceived,
        change,
        items: cart.map(item => ({
            name: item.productName || '-',
            quantity: item.quantity || 0,
            price: item.productPrice || 0,
            total: (item.productPrice || 0) * (item.quantity || 0)
        }))
    };
}

function formatDateTime(value) {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return '-';
    return date.toLocaleString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function mapPaymentMethod(method) {
    switch (method) {
        case 'CASH':
            return 'Tiền mặt';
        case 'TRANSFER':
            return 'Chuyển khoản';
        case 'CARD':
            return 'Thẻ';
        default:
            return method || '-';
    }
}


function ensureEmptyStateBrand(target) {
    const emptyState = target || document.getElementById('emptyState');
    if (!emptyState || emptyState.querySelector('.empty-brand')) return;
    const brand = document.createElement('div');
    brand.className = 'empty-brand';
    brand.innerHTML = `
        <div class="empty-brand-badge">BF</div>
        <div class="empty-brand-text">
            <div class="empty-brand-title">BizFlow</div>
            <div class="empty-brand-sub">Smart Retail POS</div>
        </div>
    `;
    emptyState.prepend(brand);
}

function renderCart() {
    const cartContainer = document.getElementById('cartItems');
    const emptyState = document.getElementById('emptyState');
    ensureEmptyStateBrand(emptyState);
    if (cart.length === 0) {
        if (cartContainer) {
            cartContainer.innerHTML = '';
        }
        if (emptyState) {
            emptyState.style.display = 'grid';
        }
        toggleEmptyState(true);
        return;
    }

    toggleEmptyState(false);
    if (emptyState) {
        emptyState.style.display = 'none';
    }
    cartContainer.innerHTML = cart.map((item, idx) => `
        <div class="cart-row ${item.isReturnItem ? 'return-item' : ''}">
            <span>${idx + 1}</span>
            <span>${item.productCode || '-'}</span>
            <span class="cart-name">${item.productName}</span>
            <span class="cart-qty ${Number(item.stock) <= 0 ? 'is-out' : ''}">
                <input type="number" class="qty-input" value="${item.quantity}" ${item.isReturnItem ? 'disabled' : ''} onchange="setQty(${idx}, this.value)">
            </span>
            <span>${item.unit || '-'}</span>
            <span>${formatPrice(item.productPrice)}</span>
            <span>${formatPrice(item.productPrice * item.quantity)}</span>
            ${item.isReturnItem ? '<span class="cart-item-locked">Đổi</span>' : `<button class="cart-item-remove" onclick="removeFromCart(${idx})">×</button>`}
        </div>
    `).join('');
}

function updateQty(idx, change) {
    if (cart[idx] && !cart[idx].isReturnItem) {
        cart[idx].quantity = Math.max(1, cart[idx].quantity + change);
        renderCart();
        updateTotal();
    }
}

function setQty(idx, value) {
    const qty = parseInt(value, 10) || 1;
    if (cart[idx] && !cart[idx].isReturnItem) {
        cart[idx].quantity = Math.max(1, qty);
        renderCart();
        updateTotal();
    }
}

function removeFromCart(idx) {
    if (cart[idx]?.isReturnItem) return;
    cart.splice(idx, 1);
    renderCart();
    updateTotal();
}

function selectCustomer(evt, customerId, customerName, customerPhone) {
    applyCustomerSelection(
        { id: customerId, name: customerName, phone: customerPhone },
        { openDetail: false, highlightEvent: evt }
    );
}

function openCustomerDetailFromButton(evt) {
    evt?.preventDefault?.();
    evt?.stopPropagation?.();
    const target = evt?.currentTarget;
    const customerId = Number(target?.dataset?.customerId);
    if (!Number.isFinite(customerId) || customerId <= 0) {
        showPopup('Không tìm thấy khách hàng.', { type: 'error' });
        return;
    }
    openCustomerDetail(customerId);
}

function clearSelectedCustomer(options = {}) {
    const { showList = true } = options;
    selectedCustomer = { id: 0, name: 'Khách lẻ', phone: '-' };
    const selectedView = document.getElementById('selectedCustomer');
    if (selectedView) {
        selectedView.textContent = 'Khách lẻ';
    }

    const searchInput = document.getElementById('customerSearch');
    if (searchInput) {
        searchInput.value = '';
        searchInput.classList.remove('has-selection');
        searchInput.readOnly = false;
    }

    const addBtn = document.getElementById('addCustomerBtn');
    if (addBtn) {
        addBtn.style.display = '';
    }
    const clearBtn = document.getElementById('clearCustomerBtn');
    if (clearBtn) {
        clearBtn.style.display = 'none';
    }

    customerSearchTerm = '';
    const customerList = document.getElementById('customerList');
    if (customerList) {
        customerList.style.display = showList ? 'block' : 'none';
    }
    if (showList) {
        applyCustomerFilter();
    }
}

function updateTotal() {
    const subtotal = cart.reduce((sum, item) => sum + (item.productPrice * item.quantity), 0);
    const total = Math.max(0, subtotal);

    document.getElementById('subtotal').textContent = formatPrice(subtotal);
    document.getElementById('totalAmount').textContent = formatPrice(total);
    document.getElementById('amountDue').textContent = formatPrice(total);
    updateChangeDue(total);
    setDefaultTierByTotal(total);
}

function getTotalAmount() {
    return cart.reduce((sum, item) => sum + (item.productPrice * item.quantity), 0);
}

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        minimumFractionDigits: 0
    }).format(price).replace('₫', 'đ');
}

function showPopup(message, options = {}) {
    const { title = 'Thông báo', type = 'info' } = options;
    let modal = document.getElementById('appPopup');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'appPopup';
        modal.className = 'app-popup';
        modal.setAttribute('aria-hidden', 'true');
        modal.innerHTML = `
            <div class="app-popup-card" role="dialog" aria-modal="true">
                <div class="app-popup-header">
                    <h3 id="appPopupTitle"></h3>
                    <button type="button" class="icon-btn small" id="appPopupClose" aria-label="Đóng">×</button>
                </div>
                <div id="appPopupMessage" class="app-popup-message"></div>
                <div class="app-popup-actions">
                    <button type="button" class="primary-btn" id="appPopupOk">Đóng</button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);

        const closePopup = () => {
            modal.classList.remove('show');
            modal.setAttribute('aria-hidden', 'true');
        };
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closePopup();
            }
        });
        modal.querySelector('#appPopupClose')?.addEventListener('click', closePopup);
        modal.querySelector('#appPopupOk')?.addEventListener('click', closePopup);
    }

    const titleEl = modal.querySelector('#appPopupTitle');
    const messageEl = modal.querySelector('#appPopupMessage');
    if (titleEl) titleEl.textContent = title;
    if (messageEl) messageEl.textContent = message || '';

    modal.classList.remove('type-info', 'type-success', 'type-error');
    modal.classList.add(`type-${type}`);
    modal.classList.add('show');
    modal.setAttribute('aria-hidden', 'false');
}

function formatPriceCompact(price) {
    return new Intl.NumberFormat('vi-VN', {
        minimumFractionDigits: 0
    }).format(price);
}

function updateChangeDue(forcedTotal = null) {
    const changeLabel = document.getElementById('changeAmount');
    if (!changeLabel) return;

    if (currentPaymentMethod !== 'CASH') {
        changeLabel.textContent = formatPrice(0);
        return;
    }

    const totalAmount = forcedTotal !== null
        ? forcedTotal
        : parseInt(document.getElementById('totalAmount').textContent.replace(/\D/g, ''), 10) || 0;
    const cashReceived = parseInt(document.getElementById('cashReceivedInput')?.value, 10) || 0;
    const change = Math.max(0, cashReceived - totalAmount);
    changeLabel.textContent = formatPrice(change);
}

function toggleCashPanel(show) {
    const panel = document.getElementById('cashPanel');
    if (!panel) return;
    panel.style.display = show ? 'block' : 'none';
}

function setDefaultTierByTotal(total) {
    const tierSelect = document.getElementById('customerTierSelect');
    if (!tierSelect) return;
    if (total >= 1000000) return;
    if (!tierSelect.value) {
        tierSelect.value = 'member';
    }
}

function setTierByPoints(points) {
    const tierSelect = document.getElementById('customerTierSelect');
    if (!tierSelect) return;

    let tier = 'member';
    if (points >= 25000) {
        tier = 'diamond';
    } else if (points >= 15000) {
        tier = 'platinum';
    } else if (points >= 9000) {
        tier = 'gold';
    } else if (points >= 3000) {
        tier = 'silver';
    }

    tierSelect.value = tier;
}

function setupEventListeners() {
    const searchInput = document.getElementById('searchInput');
    searchInput.addEventListener('input', (e) => {
        topSearchTerm = e.target.value;
        renderToolbarSearchResults(e.target.value);
    });
    searchInput.addEventListener('focus', () => {
        if (searchInput.value.trim()) {
            renderToolbarSearchResults(searchInput.value);
        }
    });
    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleBarcodeSearch();
        }
    });

    document.getElementById('suggestionSearchInput')?.addEventListener('input', (e) => {
        bottomSearchTerm = e.target.value;
        filterProducts();
    });

    document.getElementById('sortSelect').addEventListener('change', (e) => {
        currentSort = e.target.value;
        sortProducts(currentSort);
    });

    document.getElementById('qtyInput').addEventListener('change', () => {
        const val = getCurrentQty();
        document.getElementById('qtyInput').value = val;
        if (isToolbarSearchOpen()) {
            renderToolbarSearchResults(document.getElementById('searchInput').value);
        }
    });

    document.getElementById('cashReceivedInput')?.addEventListener('input', () => {
        updateChangeDue();
    });

    document.querySelectorAll('input[name="paymentMethod"]').forEach(input => {
        input.addEventListener('change', () => {
            currentPaymentMethod = input.value;
            toggleCashPanel(currentPaymentMethod === 'CASH');
            updateChangeDue();
        });
    });

    const customerSearch = document.getElementById('customerSearch');
    const clearCustomerBtn = document.getElementById('clearCustomerBtn');
    clearCustomerBtn?.addEventListener('click', () => {
        clearSelectedCustomer();
    });
    const customerList = document.getElementById('customerList');
    customerList?.addEventListener('click', (e) => {
        const row = e.target.closest('.customer-item');
        if (!row) return;
        const customerId = Number(row.dataset.customerId);
        if (!Number.isFinite(customerId)) {
            showPopup('Không tìm thấy khách hàng.', { type: 'error' });
            return;
        }
        const customerName = row.dataset.customerName || '';
        const customerPhone = row.dataset.customerPhone || '-';
        selectCustomer(e, customerId, customerName, customerPhone);
    });
    document.addEventListener('click', (e) => {
        const panel = document.querySelector('.customer-panel');
        if (!panel || !customerList) return;
        if (!panel.contains(e.target)) {
            customerList.style.display = 'none';
        }
    });
    document.addEventListener('click', (e) => {
        const toolbar = document.querySelector('.toolbar-search');
        if (!toolbar) return;
        if (!toolbar.contains(e.target)) {
            hideToolbarSearchResults();
        }
    });
    customerSearch?.addEventListener('click', () => {
        if (customerSearch.classList.contains('has-selection') && selectedCustomer?.id) {
            openCustomerDetail(selectedCustomer.id);
        }
    });
    customerSearch?.addEventListener('input', (e) => {
        customerSearchTerm = e.target.value || '';
        e.target.classList.remove('has-selection');
        const addBtn = document.getElementById('addCustomerBtn');
        if (addBtn) {
            addBtn.style.display = '';
        }
        const clearBtn = document.getElementById('clearCustomerBtn');
        if (clearBtn) {
            clearBtn.style.display = 'none';
        }
        const customerList = document.getElementById('customerList');
        if (customerList) {
            customerList.style.display = 'block';
        }
        if (!customersLoaded) {
            loadCustomers().then(() => applyCustomerFilter());
            return;
        }
        applyCustomerFilter();
    });

    document.querySelectorAll('.quick-cash .chip').forEach(btn => {
        btn.addEventListener('click', () => {
            const cashInput = document.getElementById('cashReceivedInput');
            if (cashInput) {
                cashInput.value = btn.dataset.cash;
            }
            updateChangeDue();
        });
    });

    const clearCartBtn = document.getElementById('clearCartBtn');
    clearCartBtn?.addEventListener('click', () => {
        if (confirm('X\u00f3a h\u00f3a \u0111\u01a1n n\u00e0y?')) {
            clearCart(false);
        }
    });

    setupInvoiceTabs();

    document.getElementById('saveBillBtn').addEventListener('click', () => {
        saveDraftInvoice();
    });

    document.getElementById('checkoutBtn').addEventListener('click', async () => {
        // If transfer selected, create unpaid order then show QR code
        if (currentPaymentMethod === 'TRANSFER') {
            const res = await createOrder(false);
            if (res && res.orderId) {
                const amount = res.totalAmount ? parseFloat(res.totalAmount) : getTotalAmount();
                const token = res.paymentToken || res.token || null;
                showTransferQrModal(res.orderId, amount, token);
            }
            return;
        }

        // Other methods proceed as before
        createOrder(true);
    });


    document.getElementById('logoutBtn').addEventListener('click', () => {
        if (confirm('X\u00f3a h\u00f3a \u0111\u01a1n n\u00e0y?')) {
            sessionStorage.clear();
            window.location.href = '/pages/login.html';
        }
    });

    const toolbarList = document.getElementById('toolbarSearchList');
    toolbarList?.addEventListener('click', (e) => {
        const row = e.target.closest('.toolbar-search-row.item');
        if (!row) return;
        const productId = parseInt(row.dataset.productId, 10);
        const productName = row.dataset.productName || '';
        const productPrice = parseFloat(row.dataset.productPrice) || 0;
        if (Number.isFinite(productId)) {
            addToCart(productId, productName, productPrice);
            clearToolbarSearch();
        }
    });
}

function initInvoices() {
    invoices = [];
    savedInvoices = [];
    invoiceSequence = 1;
    const initial = createInvoiceState();
    invoices.push(initial);
    activeInvoiceId = initial.id;
    renderInvoiceTabs();
    applyInvoiceState(initial);
}

function getNextInvoiceNumber() {
    let max = 0;
    invoices.forEach(inv => {
        const match = String(inv.name || '').match(/Hóa đơn\s+(\d+)/i);
        if (match) {
            max = Math.max(max, Number(match[1]));
        }
    });
    return max + 1;
}

function getNextInvoiceNumberFromAll() {
    let max = 0;
    const collect = (list) => {
        (list || []).forEach(inv => {
            const match = String(inv.name || '').match(/Hóa đơn\s+(\d+)/i);
            if (match) {
                max = Math.max(max, Number(match[1]));
            }
        });
    };
    collect(invoices);
    collect(savedInvoices);
    return max + 1;
}

function createInvoiceState(name) {
    const id = `invoice-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
    const nextNumber = getNextInvoiceNumber();
    invoiceSequence = Math.max(invoiceSequence, nextNumber + 1);
    return {
        id,
        name: name || `Hóa đơn ${nextNumber}`,
        cart: [],
        selectedCustomer: { id: 0, name: 'Khách lẻ', phone: '-' },
        paymentMethod: 'CASH',
        cashReceived: '',
        paymentNote: '',
        splitLine: false,
        topSearchTerm: '',
        bottomSearchTerm: ''
    };
}


function getActiveInvoice() {
    return invoices.find(inv => inv.id === activeInvoiceId) || null;
}

function cloneCart(items) {
    return (items || []).map(item => ({ ...item }));
}

function saveActiveInvoiceState() {
    const invoice = getActiveInvoice();
    if (!invoice) return;
    invoice.cart = cloneCart(cart);
    invoice.selectedCustomer = selectedCustomer
        ? { ...selectedCustomer }
        : { id: 0, name: 'Khách lẻ', phone: '-' };
    invoice.paymentMethod = currentPaymentMethod;
    invoice.cashReceived = document.getElementById('cashReceivedInput')?.value || '';
    invoice.paymentNote = document.getElementById('paymentNote')?.value || '';
    invoice.splitLine = document.getElementById('splitLine')?.checked || false;
    invoice.topSearchTerm = topSearchTerm || '';
    invoice.bottomSearchTerm = bottomSearchTerm || '';
}

function applyInvoiceState(invoice) {
    cart = cloneCart(invoice.cart);
    currentPaymentMethod = invoice.paymentMethod || 'CASH';
    topSearchTerm = invoice.topSearchTerm || '';
    bottomSearchTerm = invoice.bottomSearchTerm || '';
    const topSearchInput = document.getElementById('searchInput');
    if (topSearchInput) {
        topSearchInput.value = topSearchTerm;
    }
    const bottomSearchInput = document.getElementById('suggestionSearchInput');
    if (bottomSearchInput) {
        bottomSearchInput.value = bottomSearchTerm;
    }
    const splitLine = document.getElementById('splitLine');
    if (splitLine) {
        splitLine.checked = !!invoice.splitLine;
    }

    const cashInput = document.getElementById('cashReceivedInput');
    if (cashInput) {
        cashInput.value = invoice.cashReceived || '';
    }
    const noteInput = document.getElementById('paymentNote');
    if (noteInput) {
        noteInput.value = invoice.paymentNote || '';
    }

    const methodInputs = document.querySelectorAll('input[name="paymentMethod"]');
    methodInputs.forEach(input => {
        input.checked = input.value === currentPaymentMethod;
    });
    toggleCashPanel(currentPaymentMethod === 'CASH');
    updateChangeDue();

    if (invoice.selectedCustomer && invoice.selectedCustomer.id > 0) {
        applyCustomerSelection(invoice.selectedCustomer, { openDetail: false });
    } else {
        clearSelectedCustomer({ showList: false });
    }

    renderCart();
    updateTotal();
    filterProducts();
}

function renderInvoiceTabs() {
    const container = document.getElementById('orderTabs');
    if (!container) return;
    const tabs = invoices.map(invoice => `
        <button class="order-tab ${invoice.id === activeInvoiceId ? 'active' : ''}" data-invoice-id="${invoice.id}">
            ${invoice.name} <span class="tab-close" data-close="${invoice.id}">×</span>
        </button>
    `).join('');
    container.innerHTML = `
        ${tabs}
        <button class="order-tab ghost" id="addInvoiceBtn" title="Th\u00eam h\u00f3a \u0111\u01a1n">+</button>
        <button class="order-tab ghost" id="savedInvoiceBtn">H\u0110 l\u01b0u t\u1ea1m</button>
    `;
}

function setupInvoiceTabs() {
    const container = document.getElementById('orderTabs');
    if (!container) return;

    container.addEventListener('click', (e) => {
        const addBtn = e.target.closest('#addInvoiceBtn');
        if (addBtn) {
            createAndSwitchInvoice();
            return;
        }

        const savedBtn = e.target.closest('#savedInvoiceBtn');
        if (savedBtn) {
            toggleSavedBillsPanel();
            return;
        }

        const closeBtn = e.target.closest('.tab-close');
        if (closeBtn) {
            e.stopPropagation();
            const invoiceId = closeBtn.getAttribute('data-close');
            if (invoiceId && confirm('X\u00f3a h\u00f3a \u0111\u01a1n n\u00e0y?')) {
                removeInvoice(invoiceId);
            }
            return;
        }

        const tab = e.target.closest('.order-tab[data-invoice-id]');
        if (tab) {
            const invoiceId = tab.getAttribute('data-invoice-id');
            if (invoiceId) {
                switchInvoice(invoiceId);
            }
        }
    });

    const savedPanel = document.getElementById('savedBillsPanel');
    const closeSavedBtn = document.getElementById('closeSavedBills');
    closeSavedBtn?.addEventListener('click', () => toggleSavedBillsPanel(false));

    document.addEventListener('click', (e) => {
        if (!savedPanel || savedPanel.getAttribute('aria-hidden') === 'true') return;
        const savedBtn = document.getElementById('savedInvoiceBtn');
        if (savedPanel.contains(e.target) || savedBtn?.contains(e.target)) return;
        toggleSavedBillsPanel(false);
    });

    savedPanel?.addEventListener('click', (e) => {
        const openBtn = e.target.closest('[data-open-draft]');
        if (openBtn) {
            const draftId = openBtn.getAttribute('data-open-draft');
            if (draftId) {
                openSavedInvoice(draftId);
            }
        }
        const removeBtn = e.target.closest('[data-remove-draft]');
        if (removeBtn) {
            const draftId = removeBtn.getAttribute('data-remove-draft');
            if (draftId) {
                removeSavedInvoice(draftId);
            }
        }
    });
}

function createAndSwitchInvoice() {
    saveActiveInvoiceState();
    const invoice = createInvoiceState();
    invoices.push(invoice);
    activeInvoiceId = invoice.id;
    renderInvoiceTabs();
    applyInvoiceState(invoice);
}

function switchInvoice(invoiceId) {
    if (invoiceId === activeInvoiceId) return;
    saveActiveInvoiceState();
    activeInvoiceId = invoiceId;
    const invoice = getActiveInvoice();
    if (invoice) {
        renderInvoiceTabs();
        applyInvoiceState(invoice);
    }
}

function removeInvoice(invoiceId, options = {}) {
    const { resetSequence = true } = options;
    if (!invoiceId) return;
    const index = invoices.findIndex(inv => inv.id === invoiceId);
    if (index === -1) return;
    const wasActive = invoiceId === activeInvoiceId;
    invoices.splice(index, 1);
    if (invoices.length === 0) {
        if (resetSequence) {
            invoiceSequence = 1;
        }
        const fresh = createInvoiceState();
        invoices.push(fresh);
        activeInvoiceId = fresh.id;
        renderInvoiceTabs();
        applyInvoiceState(fresh);
        return;
    }
    invoiceSequence = Math.max(invoiceSequence, getNextInvoiceNumber());
    if (wasActive) {
        activeInvoiceId = invoices[0].id;
        renderInvoiceTabs();
        applyInvoiceState(invoices[0]);
    } else {
        renderInvoiceTabs();
    }
}

function saveDraftInvoice() {
    if (cart.length === 0) {
        showPopup('Giỏ hàng trống, không thể lưu tạm.', { type: 'error' });
        return;
    }
    saveActiveInvoiceState();
    const invoice = getActiveInvoice();
    if (!invoice) return;
    const draft = {
        ...invoice,
        name: `Hóa đơn ${getNextInvoiceNumberFromAll()}`,
        cart: cloneCart(invoice.cart),
        savedAt: new Date().toISOString()
    };
    savedInvoices.unshift(draft);
    removeInvoice(invoice.id, { resetSequence: false });
    renderSavedBills();
    toggleSavedBillsPanel(false);
}

function renderSavedBills() {
    const list = document.getElementById('savedBillsList');
    const empty = document.getElementById('savedBillsEmpty');
    if (!list || !empty) return;
    if (savedInvoices.length === 0) {
        list.innerHTML = '';
        empty.style.display = 'block';
        return;
    }
    empty.style.display = 'none';
    list.innerHTML = savedInvoices.map(draft => {
        const total = draft.cart.reduce((sum, item) => sum + (item.productPrice * item.quantity), 0);
        const customerName = draft.selectedCustomer?.name || 'Khách lẻ';
        return `
            <div class="saved-bill-item">
                <button class="saved-bill-open" data-open-draft="${draft.id}">
                    <strong>${draft.name}</strong>
                    <span>${customerName}</span>
                    <span>${formatPrice(total)}</span>
                </button>
                <button class="saved-bill-remove" data-remove-draft="${draft.id}">×</button>
            </div>
        `;
    }).join('');
}

function toggleSavedBillsPanel(forceState) {
    const panel = document.getElementById('savedBillsPanel');
    const button = document.getElementById('savedInvoiceBtn');
    if (!panel || !button) return;
    const show = typeof forceState === 'boolean' ? forceState : panel.getAttribute('aria-hidden') === 'true';
    if (show) {
        renderSavedBills();
        const rect = button.getBoundingClientRect();
        panel.style.top = `${rect.bottom + window.scrollY + 8}px`;
        panel.style.left = `${rect.left + window.scrollX}px`;
        panel.classList.add('show');
        panel.setAttribute('aria-hidden', 'false');
    } else {
        panel.classList.remove('show');
        panel.setAttribute('aria-hidden', 'true');
    }
}

function openSavedInvoice(draftId) {
    const index = savedInvoices.findIndex(draft => draft.id === draftId);
    if (index === -1) return;
    saveActiveInvoiceState();
    const draft = savedInvoices.splice(index, 1)[0];
    const emptyTarget = invoices.find(inv => isInvoiceEmpty(inv));
    if (emptyTarget) {
        emptyTarget.cart = cloneCart(draft.cart);
        emptyTarget.selectedCustomer = draft.selectedCustomer ? { ...draft.selectedCustomer } : { id: 0, name: 'Khách lẻ', phone: '-' };
        emptyTarget.paymentMethod = draft.paymentMethod || 'CASH';
        emptyTarget.cashReceived = draft.cashReceived || '';
        emptyTarget.paymentNote = draft.paymentNote || '';
        emptyTarget.splitLine = !!draft.splitLine;
        emptyTarget.topSearchTerm = draft.topSearchTerm || '';
        emptyTarget.bottomSearchTerm = draft.bottomSearchTerm || '';
        activeInvoiceId = emptyTarget.id;
    } else {
        const nextNumber = getNextInvoiceNumber();
        draft.name = `Hóa đơn ${nextNumber}`;
        invoiceSequence = Math.max(invoiceSequence, nextNumber + 1);
        invoices.push(draft);
        activeInvoiceId = draft.id;
    }
    renderInvoiceTabs();
    applyInvoiceState(getActiveInvoice());
    renderSavedBills();
    toggleSavedBillsPanel(false);
}

function isInvoiceEmpty(invoice) {
    if (!invoice) return false;
    const hasItems = Array.isArray(invoice.cart) && invoice.cart.length > 0;
    const hasCustomer = invoice.selectedCustomer && invoice.selectedCustomer.id > 0;
    const hasNote = (invoice.paymentNote || '').trim().length > 0;
    const hasCash = (invoice.cashReceived || '').toString().trim().length > 0;
    const hasSearch = (invoice.topSearchTerm || '').trim().length > 0 || (invoice.bottomSearchTerm || '').trim().length > 0;
    return !hasItems && !hasCustomer && !hasNote && !hasCash && !hasSearch;
}

function removeSavedInvoice(draftId) {
    const index = savedInvoices.findIndex(draft => draft.id === draftId);
    if (index === -1) return;
    savedInvoices.splice(index, 1);
    renderSavedBills();
}

function applyCustomerSelection(customer, options = {}) {
    const { openDetail = false, highlightEvent = null } = options;
    selectedCustomer = { id: customer.id, name: customer.name, phone: customer.phone };

    document.querySelectorAll('.customer-item').forEach(item => {
        item.classList.remove('active');
    });
    const row = highlightEvent?.target?.closest?.('.customer-item');
    if (row) row.classList.add('active');

    const searchInput = document.getElementById('customerSearch');
    if (searchInput) {
        searchInput.value = customer.name || customer.phone || '';
        searchInput.classList.add('has-selection');
        searchInput.readOnly = true;
    }

    customerSearchTerm = '';
    const customerList = document.getElementById('customerList');
    if (customerList) {
        customerList.innerHTML = '';
        customerList.style.display = 'none';
    }

    const addBtn = document.getElementById('addCustomerBtn');
    if (addBtn) {
        addBtn.style.display = 'none';
    }
    const clearBtn = document.getElementById('clearCustomerBtn');
    if (clearBtn) {
        clearBtn.style.display = 'inline-flex';
    }

    if (openDetail && customer.id) {
        openCustomerDetail(customer.id);
    }
}

function setupCustomerModal() {
    const addButton = document.getElementById('addCustomerBtn');
    const modal = document.getElementById('customerModal');
    const closeBtn = document.getElementById('closeCustomerModal');
    const cancelBtn = document.getElementById('cancelCustomerModal');
    const form = document.getElementById('customerForm');
    const pointsInput = document.getElementById('customerPointsInput');
    const cityInput = document.getElementById('customerCityInput');
    const districtInput = document.getElementById('customerDistrictInput');
    const wardInput = document.getElementById('customerWardInput');

    if (!addButton || !modal || !closeBtn || !form) return;

    addButton.addEventListener('click', () => {
        const searchValue = document.getElementById('customerSearch')?.value.trim();
        const normalized = (searchValue || '').replace(/\s+/g, '');
        if (normalized) {
            const existing = customers.find(c => (c.phone || '').replace(/\s+/g, '') === normalized);
            if (existing) {
                selectCustomer(null, existing.id, existing.name, existing.phone || '-');
                return;
            }
        }
        openCustomerModalWithPhone(searchValue || '');
    });

    closeBtn.addEventListener('click', () => closeCustomerModal());
    cancelBtn?.addEventListener('click', () => closeCustomerModal());

    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            closeCustomerModal();
        }
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        await createCustomerFromForm();
    });

    pointsInput?.addEventListener('input', () => {
        const raw = parseInt(pointsInput.value, 10) || 0;
        setTierByPoints(raw);
    });

    cityInput?.addEventListener('focus', () => {
        renderDatalistOptions(document.getElementById('customerCityList'), cityCache);
    });

    cityInput?.addEventListener('input', () => {
        filterDatalist('customerCityList', cityCache, cityInput.value);
        const match = findMatchByName(cityCache, cityInput.value);
        if (match && cityInput.dataset.code !== String(match.code)) {
            cityInput.value = match.displayName || match.name;
            cityInput.dataset.code = match.code;
            resetAddressInput(districtInput);
            resetAddressInput(wardInput);
            loadDistricts(match.code);
            return;
        }
        if (!match) {
            cityInput.dataset.code = '';
            resetAddressInput(districtInput);
            resetAddressInput(wardInput);
        }
    });

    districtInput?.addEventListener('focus', () => {
        renderDatalistOptions(document.getElementById('customerDistrictList'), districtCache);
    });

    districtInput?.addEventListener('input', () => {
        filterDatalist('customerDistrictList', districtCache, districtInput.value);
        const match = findMatchByName(districtCache, districtInput.value);
        if (match && districtInput.dataset.code !== String(match.code)) {
            districtInput.value = match.displayName || match.name;
            districtInput.dataset.code = match.code;
            resetAddressInput(wardInput);
            loadWards(match.code);
            return;
        }
        if (!match) {
            districtInput.dataset.code = '';
            resetAddressInput(wardInput);
        }
    });

    wardInput?.addEventListener('focus', () => {
        renderDatalistOptions(document.getElementById('customerWardList'), wardCache);
    });

    wardInput?.addEventListener('input', () => {
        filterDatalist('customerWardList', wardCache, wardInput.value);
        const match = findMatchByName(wardCache, wardInput.value);
        if (match) {
            wardInput.value = match.displayName || match.name;
            wardInput.dataset.code = match.code;
        } else {
            wardInput.dataset.code = '';
        }
    });
}

function setupProductDetailModal() {
    const modal = document.getElementById('productDetailModal');
    if (!modal) return;

    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            closeProductDetail();
        }
    });
}

function setupCustomerDetailModal() {
    const modal = document.getElementById('customerDetailModal');
    const selectedView = document.getElementById('selectedCustomer');
    if (!modal || !selectedView) return;

    selectedView.addEventListener('click', () => {
        if (!selectedCustomer || !selectedCustomer.id) return;
        openCustomerDetail(selectedCustomer.id);
    });

    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            closeCustomerDetail();
        }
    });
}

function openCustomerModalWithPhone(phone) {
    const modal = document.getElementById('customerModal');
    const form = document.getElementById('customerForm');
    if (!modal || !form) return;

    modal.classList.add('show');
    modal.setAttribute('aria-hidden', 'false');
    form.reset();
    const phoneInput = document.getElementById('customerPhoneInput');
    if (phoneInput) {
        phoneInput.value = phone || document.getElementById('customerSearch')?.value.trim() || '';
    }
    const confirmInput = document.getElementById('customerConfirmInput');
    if (confirmInput) {
        confirmInput.checked = false;
    }
    const cityInput = document.getElementById('customerCityInput');
    const districtInput = document.getElementById('customerDistrictInput');
    const wardInput = document.getElementById('customerWardInput');
    if (cityInput) {
        cityInput.value = '';
        cityInput.dataset.code = '';
    }
    resetAddressInput(districtInput);
    resetAddressInput(wardInput);
    const districtList = document.getElementById('customerDistrictList');
    const wardList = document.getElementById('customerWardList');
    if (districtList) districtList.innerHTML = '';
    if (wardList) wardList.innerHTML = '';

    document.getElementById('customerNameInput')?.focus();
    loadCities();
}

function closeCustomerModal() {
    const modal = document.getElementById('customerModal');
    if (!modal) return;
    modal.classList.remove('show');
    modal.setAttribute('aria-hidden', 'true');
}

async function createCustomerFromForm() {
    const name = document.getElementById('customerNameInput')?.value.trim();
    const phone = document.getElementById('customerPhoneInput')?.value.trim();
    const email = document.getElementById('customerEmailInput')?.value.trim();
    const address = document.getElementById('customerAddressInput')?.value.trim();
    const cityInput = document.getElementById('customerCityInput');
    const districtInput = document.getElementById('customerDistrictInput');
    const wardInput = document.getElementById('customerWardInput');
    const confirmed = document.getElementById('customerConfirmInput')?.checked;

    if (!name) {
        showPopup('Vui lòng nhập tên khách hàng.', { type: 'error' });
        return;
    }

    const normalizedPhone = (phone || '').replace(/\s+/g, '');
    if (normalizedPhone) {
        const existing = customers.find(c => (c.phone || '').replace(/\s+/g, '') === normalizedPhone);
        if (existing) {
            selectCustomer(null, existing.id, existing.name, existing.phone || '-');
            closeCustomerModal();
            return;
        }
    }

    if (!phone) {
        showPopup('Vui lòng nhập số điện thoại.', { type: 'error' });
        return;
    }
    if (!/^\d{9,11}$/.test(phone)) {
        showPopup('Số điện thoại phải là 9-11 chữ số.', { type: 'error' });
        return;
    }

    const cityCode = cityInput?.dataset.code || '';
    const districtCode = districtInput?.dataset.code || '';
    const wardCode = wardInput?.dataset.code || '';
    if (!cityInput?.value || !districtInput?.value || !wardInput?.value || !address || !cityCode || !districtCode || !wardCode) {
        showPopup('Vui lòng nhập đầy đủ địa chỉ.', { type: 'error' });
        return;
    }

    if (!confirmed) {
        showPopup('Vui lòng xác nhận thông tin khách hàng.', { type: 'error' });
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/customers`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${sessionStorage.getItem('accessToken') || ''}`
            },
            body: JSON.stringify({
                name,
                phone: phone || null,
                email: email || null,
                address: address || null
            })
        });

    if (!res.ok) {
        const message = await res.text();
        showPopup(message || 'Không thể tạo khách hàng.', { type: 'error' });
        return;
    }

    const created = await res.json();
        customers.unshift(created);
        customersLoaded = true;
        customerSearchTerm = '';
        const searchInput = document.getElementById('customerSearch');
    if (searchInput) searchInput.value = '';
    applyCustomerFilter();
    closeCustomerModal();
    selectCustomer(null, created.id, created.name, created.phone || '-');
    if (searchInput) {
        searchInput.value = created.name || created.phone || '';
        searchInput.classList.add('has-selection');
    }
    } catch (err) {
        showPopup('Lỗi kết nối khi tạo khách hàng.', { type: 'error' });
    }
}

async function loadCities() {
    const cityList = document.getElementById('customerCityList');
    if (!cityList || cityList.children.length > 0) return;

    try {
        const res = await fetch('https://provinces.open-api.vn/api/p/');
        if (!res.ok) return;
        const data = await res.json();
        cityCache = data || [];
        renderDatalistOptions(cityList, cityCache);
    } catch (err) {
    }
}

async function loadDistricts(cityCode) {
    const districtInput = document.getElementById('customerDistrictInput');
    const districtList = document.getElementById('customerDistrictList');
    if (!districtInput || !districtList) return;
    districtInput.disabled = true;
    try {
        const res = await fetch(`https://provinces.open-api.vn/api/p/${cityCode}?depth=2`);
        if (!res.ok) return;
        const data = await res.json();
        districtCache = data.districts || [];
        renderDatalistOptions(districtList, districtCache);
        districtInput.disabled = false;
    } catch (err) {
    }
}

async function loadWards(districtCode) {
    const wardInput = document.getElementById('customerWardInput');
    const wardList = document.getElementById('customerWardList');
    if (!wardInput || !wardList) return;
    wardInput.disabled = true;
    try {
        const res = await fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`);
        if (!res.ok) return;
        const data = await res.json();
        wardCache = data.wards || [];
        renderDatalistOptions(wardList, wardCache);
        wardInput.disabled = false;
    } catch (err) {
    }
}

function resetAddressInput(input) {
    if (!input) return;
    input.value = '';
    input.dataset.code = '';
    input.disabled = true;
}

function getAddressDisplayName(item) {
    const rawName = item?.name || '';
    const cleaned = rawName.replace(/^(Tỉnh|Thành phố)\s+/i, '').trim();
    return cleaned || rawName;
}

function getAddressVariants(item) {
    const rawName = item?.name || '';
    const displayName = getAddressDisplayName(item);
    const variants = [displayName];
    if (displayName !== rawName) {
        variants.push(rawName);
    }
    return variants;
}

function renderDatalistOptions(listEl, items) {
    if (!listEl) return;
    listEl.innerHTML = '';
    const seen = new Set();
    (items || []).forEach(item => {
        const displayName = getAddressDisplayName(item);
        if (!displayName || seen.has(displayName)) return;
        const option = document.createElement('option');
        option.value = displayName;
        listEl.appendChild(option);
        seen.add(displayName);
    });
}

function findMatchByName(items, value) {
    const normalized = normalizeKeyword(value);
    if (!normalized) return null;
    const list = items || [];
    for (const item of list) {
        const variants = getAddressVariants(item);
        for (const variant of variants) {
            if (normalizeKeyword(variant) === normalized) {
                return {
                    ...item,
                    displayName: variant
                };
            }
        }
    }
    return null;
}

function filterDatalist(listId, items, query) {
    const listEl = document.getElementById(listId);
    if (!listEl) return;
    const normalizedQuery = normalizeKeyword(query);
    listEl.innerHTML = '';
    const seen = new Set();
    (items || []).forEach(item => {
        const variants = getAddressVariants(item);
        const matches = !normalizedQuery || variants.some(variant => (
            normalizeKeyword(variant).includes(normalizedQuery)
        ));
        if (!matches) return;
        const displayName = getAddressDisplayName(item);
        if (!displayName || seen.has(displayName)) return;
        const option = document.createElement('option');
        option.value = displayName;
        listEl.appendChild(option);
        seen.add(displayName);
    });
}

function filterProducts(searchTerm = '') {
    const explicitKeyword = normalizeKeyword(searchTerm);
    const bottomKeyword = normalizeKeyword(bottomSearchTerm);

    if (bottomKeyword) {
        const filtered = applySort(filterProductList(products, bottomKeyword));
        setSuggestionMode('bottom');
        renderProducts(filtered, 'detailed');
        return;
    }

    setSuggestionMode('default');
    renderProducts(applySort(getBestSellerProducts()), 'default');
}

function sortProducts(sortBy) {
    currentSort = sortBy;
    filterProducts();
}

function getBestSellerProducts() {
    return [...products]
        .sort((a, b) => (a.id || 0) - (b.id || 0))
        .slice(0, 10);
}

function applySort(list) {
    const sorted = [...(list || [])];
    switch (currentSort) {
        case 'price-low':
            sorted.sort((a, b) => (a.price || 0) - (b.price || 0));
            break;
        case 'price-high':
            sorted.sort((a, b) => (b.price || 0) - (a.price || 0));
            break;
        case 'name':
        default:
            sorted.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
            break;
    }
    return sorted;
}

function getCurrentQty() {
    const raw = parseInt(document.getElementById('qtyInput').value, 10);
    return Number.isFinite(raw) && raw > 0 ? raw : 1;
}

function stripDiacritics(value) {
    return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

function normalizeKeyword(value) {
    return stripDiacritics((value || '').toString().trim().toLowerCase());
}

function escapeHtml(value) {
    return (value || '').replace(/[&<>"']/g, (char) => {
        switch (char) {
            case '&':
                return '&amp;';
            case '<':
                return '&lt;';
            case '>':
                return '&gt;';
            case '"':
                return '&quot;';
            case '\'':
                return '&#39;';
            default:
                return char;
        }
    });
}

function escapeForSingleQuote(value) {
    return (value || '').replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}

function getEffectivePrice(product) {
    const basePrice = Number(product?.price);
    const direct = Number(
        product?.promoPrice ??
        product?.promotionPrice ??
        product?.discountPrice ??
        product?.salePrice ??
        product?.discountedPrice
    );
    const percent = Number(
        product?.discountPercent ??
        product?.discountRate ??
        product?.promoPercent ??
        product?.salePercent
    );

    if (Number.isFinite(direct)) {
        return direct;
    }

    if (Number.isFinite(basePrice) && Number.isFinite(percent) && percent > 0) {
        return Math.max(0, basePrice * (1 - percent / 100));
    }

    return Number.isFinite(basePrice) ? basePrice : NaN;
}

function filterProductList(list, keyword) {
    return list.filter(p => {
        const matchCategory = currentCategory === 'all' || p.categoryId === parseInt(currentCategory, 10);
        const matchSearch = productMatchesKeyword(p, keyword);
        return matchCategory && matchSearch;
    });
}

function productMatchesKeyword(product, keyword) {
    const normalized = normalizeKeyword(keyword);
    if (!normalized) return true;
    const name = normalizeKeyword(product.name);
    const code = normalizeKeyword(product.code);
    const barcode = normalizeKeyword(product.barcode);
    return name.includes(normalized) || code.includes(normalized) || barcode.includes(normalized);
}

function handleBarcodeSearch() {
    const input = document.getElementById('searchInput');
    if (!input) return;
    const keyword = normalizeKeyword(input.value);
    if (!keyword) return;

    const exactMatch = products.find(p =>
        normalizeKeyword(p.code) === keyword ||
        normalizeKeyword(p.barcode) === keyword
    );
    if (exactMatch) {
        addToCart(exactMatch.id, exactMatch.name, exactMatch.price || 0);
        clearToolbarSearch();
        return;
    }

    const nameMatch = products.find(p => normalizeKeyword(p.name) === keyword);
    if (nameMatch) {
        addToCart(nameMatch.id, nameMatch.name, nameMatch.price || 0);
        clearToolbarSearch();
        return;
    }

    renderToolbarSearchResults(input.value);
}

function getStockValue(product) {
    const candidates = [product.stock, product.quantity, product.inventory, product.onHand];
    const found = candidates.find(val => Number.isFinite(Number(val)));
    return Number.isFinite(Number(found)) ? Number(found) : 0;
}

function renderProducts(filteredProducts = null, viewMode = 'default') {
    const displayProducts = filteredProducts || products;
    const grid = document.getElementById('productsGrid');

    if (!displayProducts || displayProducts.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #999;">Không có sản phẩm</div>';
        return;
    }

    if (viewMode === 'compact') {
        grid.innerHTML = displayProducts.map(p => `
            <div class="product-card compact" onclick="addToCart(${p.id}, '${p.name}', ${p.price || 0})">
                <div class="product-name">${p.name || 'Sản phẩm'}</div>
                <div class="product-sku">${p.code || p.barcode || 'SKU'}</div>
                <div class="product-stock">Tồn: ${getStockValue(p)}</div>
            </div>
        `).join('');
        return;
    }

    if (viewMode === 'detailed') {
        grid.innerHTML = displayProducts.map(p => `
            <div class="product-card detailed" onclick="openProductDetail(${p.id})">
                <div class="product-price-tag">${formatPriceCompact(p.price || 0)}</div>
                <div class="product-image">${PRODUCT_ICON}</div>
                <div class="product-name">${p.name || 'Sản phẩm'}</div>
                <div class="product-sku">${p.code || p.barcode || 'SKU'}</div>
                <div class="product-meta">
                    <span>${p.unit ? `ĐVT: ${p.unit}` : 'ĐVT: -'}</span>
                    <span>Tồn: ${getStockValue(p)}</span>
                </div>
                <div class="product-description">${p.description || 'Chưa có mô tả'}</div>
            </div>
        `).join('');
        return;
    }

    grid.innerHTML = displayProducts.map(p => `
        <div class="product-card" onclick="addToCart(${p.id}, '${p.name}', ${p.price || 0})">
            <div class="product-price-tag">${formatPriceCompact(p.price || 0)}</div>
            <div class="product-image">${PRODUCT_ICON}</div>
            <div class="product-name">${p.name || 'Sản phẩm'}</div>
            <div class="product-sku">${p.code || 'SKU'}</div>
        </div>
    `).join('');
}

function setSuggestionMode(mode) {
    const title = document.getElementById('suggestionTitle');
    const controls = document.getElementById('suggestionControls');

    if (!controls || !title) return;

    if (mode === 'bottom') {
        controls.style.display = 'flex';
        title.textContent = 'TƯ VẤN BÁN HÀNG';
        return;
    }

    if (mode === 'top') {
        controls.style.display = 'none';
        title.textContent = 'KẾT QUẢ TÌM KIẾM';
        return;
    }

    controls.style.display = 'flex';
    title.textContent = 'SẢN PHẨM BÁN CHẠY';
}

function openProductDetail(productId) {
    const product = products.find(p => p.id === productId);
    if (!product) return;

    const modal = document.getElementById('productDetailModal');
    if (!modal) return;

    document.getElementById('detailProductName').textContent = product.name || 'Sản phẩm';
    document.getElementById('detailProductSku').textContent = product.code || product.barcode || '-';
    document.getElementById('detailProductBarcode').textContent = product.barcode || '-';
    document.getElementById('detailProductUnit').textContent = product.unit || '-';
    document.getElementById('detailProductPrice').textContent = formatPrice(getEffectivePrice(product) || 0);
    document.getElementById('detailProductStock').textContent = getStockValue(product);
    document.getElementById('detailProductDescription').textContent = product.description || 'Chưa có mô tả';

    const addBtn = document.getElementById('detailAddToCart');
    if (addBtn) {
        addBtn.onclick = () => {
            addToCart(product.id, product.name, product.price || 0);
            closeProductDetail();
        };
    }

    modal.classList.add('show');
    modal.setAttribute('aria-hidden', 'false');
}

function closeProductDetail() {
    const modal = document.getElementById('productDetailModal');
    if (!modal) return;
    modal.classList.remove('show');
    modal.setAttribute('aria-hidden', 'true');
}

function isToolbarSearchOpen() {
    const panel = document.getElementById('toolbarSearchResults');
    return panel?.classList.contains('show');
}

function showToolbarSearchResults() {
    const panel = document.getElementById('toolbarSearchResults');
    if (!panel) return;
    panel.classList.add('show');
    panel.setAttribute('aria-hidden', 'false');
}

function hideToolbarSearchResults() {
    const panel = document.getElementById('toolbarSearchResults');
    if (!panel) return;
    panel.classList.remove('show');
    panel.setAttribute('aria-hidden', 'true');
}

function clearToolbarSearch() {
    const input = document.getElementById('searchInput');
    if (input) {
        input.value = '';
    }
    topSearchTerm = '';
    hideToolbarSearchResults();
}

function renderToolbarSearchResults(rawKeyword) {
    const keyword = normalizeKeyword(rawKeyword);
    const panel = document.getElementById('toolbarSearchResults');
    const list = document.getElementById('toolbarSearchList');
    const empty = document.getElementById('toolbarSearchEmpty');
    if (!panel || !list || !empty) return;

    if (!keyword) {
        list.innerHTML = '';
        empty.style.display = 'none';
        hideToolbarSearchResults();
        return;
    }

    const matches = applySort(filterProductList(products, keyword));
    const qty = getCurrentQty();
    if (!matches || matches.length === 0) {
        list.innerHTML = '';
        empty.style.display = 'block';
        showToolbarSearchResults();
        return;
    }

    empty.style.display = 'none';
    list.innerHTML = matches.map((p, idx) => {
        const name = p.name || 'Sản phẩm';
        const code = p.code || p.barcode || '-';
        const sku = p.sku || p.skuCode || p.skuId || p.code || p.barcode || '-';
        const unit = p.unit || '-';
        const price = getEffectivePrice(p) || 0;
        const total = price * qty;
        return `
            <button type="button" class="toolbar-search-row item"
                data-product-id="${p.id}"
                data-product-name="${escapeHtml(name)}"
                data-product-price="${price}">
                <span>${idx + 1}</span>
                <span>${escapeHtml(code)}</span>
                <span>${escapeHtml(sku)}</span>
                <span class="toolbar-search-name">${escapeHtml(name)}</span>
                <span>${qty}</span>
                <span>${escapeHtml(unit)}</span>
                <span>${formatPrice(price)}</span>
                <span>${formatPrice(total)}</span>
            </button>
        `;
    }).join('');
    showToolbarSearchResults();
}

function openCustomerDetail(customerId) {
    const modal = document.getElementById('customerDetailModal');
    if (!modal) return;

    const customer = customers.find(c => c.id === customerId);
    if (!customer) {
        showPopup('Không tìm thấy khách hàng.', { type: 'error' });
        return;
    }

    document.getElementById('detailCustomerName').textContent = (customer.name || '').toUpperCase();
    const nameCard = document.getElementById('detailCustomerNameCard');
    if (nameCard) {
        nameCard.textContent = customer.name || '-';
    }
    document.getElementById('detailCustomerPhone').textContent = customer.phone || '-';
    document.getElementById('detailCustomerEmail').textContent = customer.email || '-';
    document.getElementById('detailCustomerAddress').textContent = customer.address || '-';

    modal.classList.add('show');
    modal.setAttribute('aria-hidden', 'false');
}

function closeCustomerDetail() {
    const modal = document.getElementById('customerDetailModal');
    if (!modal) return;
    modal.classList.remove('show');
    modal.setAttribute('aria-hidden', 'true');
}

function toggleEmptyState(isEmpty) {
    const emptyState = document.getElementById('emptyState');
    if (!emptyState) return;
    emptyState.style.display = isEmpty ? 'grid' : 'none';
}

function setupEmployeeSelector() {
    const button = document.getElementById('employeeSelector');
    const dropdown = document.getElementById('employeeList');
    
    if (!button || !dropdown) return;

    button.addEventListener('click', (e) => {
        e.stopPropagation();
        const isOpen = dropdown.style.display !== 'none';
        dropdown.style.display = isOpen ? 'none' : 'block';
        
        if (!isOpen && !employeesLoaded) {
            loadEmployees();
        }
    });

    document.addEventListener('click', (e) => {
        if (!dropdown || !button) return;
        if (!button.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });
}

async function loadEmployees() {
    if (employeesLoaded) return;

    const dropdown = document.getElementById('employeeList');
    if (!dropdown) return;

    dropdown.innerHTML = '<div class="employee-empty">Đang tải...</div>';

    try {
        const response = await fetch(`${API_BASE}/users`, {
            headers: { 'Authorization': `Bearer ${sessionStorage.getItem('accessToken')}` }
        });

        if (!response.ok) {
            if (response.status === 401) {
                sessionStorage.clear();
                window.location.href = '/pages/login.html';
                return;
            }
            throw new Error('Failed to load employees');
        }

        const allUsers = await response.json();
        // Filter for users with EMPLOYEE role (exclude ADMIN, OWNER, MANAGER)
        employees = allUsers.filter(u => {
            const userRole = u.role ? (typeof u.role === 'string' ? u.role : u.role.name || '') : '';
            return userRole === 'EMPLOYEE';
        });
        employeesLoaded = true;
        renderEmployees();
    } catch (err) {
        dropdown.innerHTML = '<div class="employee-empty">Lỗi tải danh sách nhân viên</div>';
    }
}

function renderEmployees() {
    const dropdown = document.getElementById('employeeList');
    if (!dropdown) return;

    if (!employees || employees.length === 0) {
        dropdown.innerHTML = '<div class="employee-empty">Chưa có nhân viên</div>';
        return;
    }

    const employeesHtml = employees.map(emp => {
        const roleDisplay = emp.role ? (typeof emp.role === 'object' && emp.role.displayName ? emp.role.displayName : 'Nhân viên') : 'Nhân viên';
        return `
        <div class="employee-item" data-employee-id="${emp.id}" onclick="selectEmployee(event, ${emp.id}, '${emp.username.replace(/'/g, "\\'")}', '${(emp.fullName || emp.username).replace(/'/g, "\\'")}')">
            <div class="employee-info">
                <p class="employee-name">${emp.fullName || emp.username}</p>
                <p class="employee-role">${roleDisplay}</p>
            </div>
        </div>
        `;
    }).join('');

    dropdown.innerHTML = employeesHtml || '<div class="employee-empty">Chưa có nhân viên</div>';
}

function selectEmployee(evt, employeeId, employeeUsername, employeeName) {
    selectedEmployee = { id: employeeId, username: employeeUsername, name: employeeName };

    const button = document.getElementById('employeeSelector');
    if (button) {
        const nameSpan = button.querySelector('span:not(.chip-caret)');
        if (nameSpan) {
            nameSpan.textContent = employeeName || employeeUsername;
        }
    }

    const dropdown = document.getElementById('employeeList');
    if (dropdown) {
        dropdown.style.display = 'none';
    }

    document.querySelectorAll('.employee-item').forEach(item => {
        item.classList.remove('active');
    });
    const row = evt?.target?.closest('.employee-item');
    if (row) row.classList.add('active');
}
