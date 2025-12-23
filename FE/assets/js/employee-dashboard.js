// Employee Dashboard - Sales JavaScript
// Use relative path so it works behind Nginx proxy inside Docker
const API_BASE = '/api';

// Global state
let products = [];
let cart = [];
let selectedCustomer = null;
let currentCategory = 'all';
let currentSearch = '';
let currentSort = 'name';
let customersLoaded = false;

// Initialize
window.addEventListener('DOMContentLoaded', async () => {
    checkAuth();
    loadUserInfo();
    // default selected customer is walk-in
    selectedCustomer = { id: 0, name: 'Khách lẻ', phone: '-' };
    document.getElementById('selectedCustomer').textContent = 'Khách lẻ';
    setupEventListeners();
    
    // Show loading state
    document.getElementById('productsGrid').innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #999;">⏳ Đang tải...</div>';
    
    // Load data in parallel for faster load
    await Promise.all([loadProducts()]);
    
    // Lazy load customers only when needed
    document.getElementById('customerSearch')?.addEventListener('focus', () => {
        if (!customersLoaded) loadCustomers();
    }, { once: true });
});

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
        console.error('Error loading products:', err);
        renderProducts([]); // Show empty state
    }
}

let customersLoaded = false;

async function loadCustomers() {
    if (customersLoaded) return; // Prevent duplicate load
    
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
        
        const customers = await response.json();
        renderCustomers(customers);
        customersLoaded = true;
    } catch (err) {
        console.error('Error loading customers:', err);
    }
}

function renderProducts(filteredProducts = null) {
    const displayProducts = filteredProducts || products;
    const grid = document.getElementById('productsGrid');
    
    if (!displayProducts || displayProducts.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #999;">Không có sản phẩm</div>';
        return;
    }
    
    grid.innerHTML = displayProducts.map(p => `
        <div class="product-card">
            <div class="product-image">📦</div>
            <div class="product-name">${p.name || 'Sản phẩm'}</div>
            <div class="product-sku">${p.code || 'SKU'}</div>
            <div class="product-price">${formatPrice(p.price || 0)}</div>
            <button class="product-button" onclick="addToCart(${p.id}, '${p.name}', ${p.price || 0})">
                Thêm vào giỏ
            </button>
        </div>
    `).join('');
}

function renderCustomers(customers) {
    const list = document.getElementById('customerList');
    if (!customers || customers.length === 0) {
        return;
    }
    
    const customersHtml = customers.map(c => `
        <div class="customer-item" data-customer-id="${c.id}" onclick="selectCustomer(event, ${c.id}, '${c.name}', '${c.phone || '-'}')">
            <div class="customer-info">
                <p class="customer-name">${c.name || 'Khách hàng'}</p>
                <p class="customer-phone">${c.phone || '-'}</p>
            </div>
        </div>
    `).join('');
    
    list.innerHTML = `
        <div class="customer-item active" data-customer-id="0" onclick="selectCustomer(event, 0, 'Khách lẻ', '-')">
            <div class="customer-info">
                <p class="customer-name">Khách lẻ</p>
                <p class="customer-phone">-</p>
            </div>
        </div>
        ${customersHtml}
    `;
}

function addToCart(productId, productName, productPrice) {
    const existingItem = cart.find(item => item.productId === productId);
    const qty = getCurrentQty();
    
    if (existingItem) {
        existingItem.quantity += qty;
    } else {
        cart.push({
            productId,
            productName,
            productPrice,
            quantity: qty
        });
    }
    
    renderCart();
    updateTotal();
}

function renderCart() {
    const cartContainer = document.getElementById('cartItems');
    
    if (cart.length === 0) {
        cartContainer.innerHTML = '<div class="empty-cart"><p>Giỏ hàng trống</p></div>';
        return;
    }
    
    cartContainer.innerHTML = cart.map((item, idx) => `
        <div class="cart-item">
            <div class="cart-item-info">
                <div class="cart-item-name">${item.productName}</div>
                <div class="cart-item-price">${formatPrice(item.productPrice)}</div>
                <div class="cart-item-qty">
                    <button class="qty-btn" onclick="updateQty(${idx}, -1)">−</button>
                    <input type="number" class="qty-input" value="${item.quantity}" onchange="setQty(${idx}, this.value)">
                    <button class="qty-btn" onclick="updateQty(${idx}, 1)">+</button>
                </div>
            </div>
            <button class="cart-item-remove" onclick="removeFromCart(${idx})">×</button>
        </div>
    `).join('');
}

function updateQty(idx, change) {
    if (cart[idx]) {
        cart[idx].quantity = Math.max(1, cart[idx].quantity + change);
        renderCart();
        updateTotal();
    }
}

function setQty(idx, value) {
    const qty = parseInt(value) || 1;
    if (cart[idx]) {
        cart[idx].quantity = Math.max(1, qty);
        renderCart();
        updateTotal();
    }
}

function removeFromCart(idx) {
    cart.splice(idx, 1);
    renderCart();
    updateTotal();
}

function selectCustomer(evt, customerId, customerName, customerPhone) {
    selectedCustomer = { id: customerId, name: customerName, phone: customerPhone };
    
    document.querySelectorAll('.customer-item').forEach(item => {
        item.classList.remove('active');
    });
    const row = evt?.target?.closest('.customer-item');
    if (row) row.classList.add('active');
    
    document.getElementById('selectedCustomer').innerHTML = `
        <div>
            <p class="customer-name">${customerName}</p>
            <p class="customer-phone">${customerPhone}</p>
        </div>
    `;
}

function updateTotal() {
    const subtotal = cart.reduce((sum, item) => sum + (item.productPrice * item.quantity), 0);
    const discount = parseInt(document.getElementById('discountInput').value) || 0;
    const total = Math.max(0, subtotal - discount);
    
    document.getElementById('subtotal').textContent = formatPrice(subtotal);
    document.getElementById('totalAmount').textContent = formatPrice(total);
}

function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        minimumFractionDigits: 0
    }).format(price).replace('₫', 'đ');
}

function setupEventListeners() {
    // Category chips
    document.querySelectorAll('#categoryChips .chip').forEach(btn => {
        btn.addEventListener('click', (e) => {
            document.querySelectorAll('#categoryChips .chip').forEach(b => b.classList.remove('active'));
            e.target.classList.add('active');
            currentCategory = e.target.dataset.category;
            filterProducts();
        });
    });

    // Search
    document.getElementById('searchInput').addEventListener('input', (e) => {
        currentSearch = e.target.value;
        filterProducts();
    });

    // Sort
    document.getElementById('sortSelect').addEventListener('change', (e) => {
        currentSort = e.target.value;
        sortProducts(currentSort);
    });

    // Qty change
    document.getElementById('qtyInput').addEventListener('change', () => {
        const val = getCurrentQty();
        document.getElementById('qtyInput').value = val;
    });
    
    // Discount input
    document.getElementById('discountInput').addEventListener('input', () => {
        updateTotal();
    });

    // Quick cash buttons (set discount for demo)
    document.querySelectorAll('.quick-cash .chip').forEach(btn => {
        btn.addEventListener('click', () => {
            document.getElementById('discountInput').value = btn.dataset.cash;
            updateTotal();
        });
    });
    
    // Cart buttons
    document.getElementById('clearCartBtn').addEventListener('click', () => {
        if (confirm('Xóa tất cả sản phẩm trong giỏ?')) {
            cart = [];
            renderCart();
            updateTotal();
        }
    });
    
    document.getElementById('saveBillBtn').addEventListener('click', () => {
        if (cart.length === 0) {
            alert('Giỏ hàng trống!');
            return;
        }
        const cust = selectedCustomer?.name || 'Khách lẻ';
        alert(`Lưu hóa đơn:\n- Khách: ${cust}\n- ${cart.length} sản phẩm\n- Tổng: ${document.getElementById('totalAmount').textContent}`);
    });
    
    document.getElementById('checkoutBtn').addEventListener('click', () => {
        if (cart.length === 0) {
            alert('Giỏ hàng trống!');
            return;
        }
        const cust = selectedCustomer?.name || 'Khách lẻ';
        alert(`Thanh toán:\n- Khách: ${cust}\n- ${cart.length} sản phẩm\n- Tổng: ${document.getElementById('totalAmount').textContent}`);
    });
    
    // Logout
    document.getElementById('logoutBtn').addEventListener('click', () => {
        if (confirm('Đăng xuất?')) {
            sessionStorage.clear();
            window.location.href = '/pages/login.html';
        }
    });
}

function filterProducts(searchTerm = '') {
    const keyword = searchTerm || currentSearch;
    let filtered = products.filter(p => {
        const matchCategory = currentCategory === 'all' || p.categoryId === parseInt(currentCategory);
        const matchSearch = !keyword || 
                           (p.name && p.name.toLowerCase().includes(keyword.toLowerCase())) ||
                           (p.code && p.code.toLowerCase().includes(keyword.toLowerCase()));
        return matchCategory && matchSearch;
    });

    renderProducts(filtered);
}

function sortProducts(sortBy) {
    switch (sortBy) {
        case 'name':
            products.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
            break;
        case 'price-low':
            products.sort((a, b) => (a.price || 0) - (b.price || 0));
            break;
        case 'price-high':
            products.sort((a, b) => (b.price || 0) - (a.price || 0));
            break;
    }
    
    filterProducts();
}

function getCurrentQty() {
    const raw = parseInt(document.getElementById('qtyInput').value, 10);
    return Number.isFinite(raw) && raw > 0 ? raw : 1;
}
