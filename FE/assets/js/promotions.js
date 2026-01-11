const API_BASE = resolveApiBase();

let promotions = [];
let products = [];
let promoProducts = [];
let searchTerm = '';
let filterType = 'ALL';

window.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadUserInfo();
    setupActions();
    loadPromotions();
});

function resolveApiBase() {
    const configured = window.API_BASE_URL || window.API_BASE;
    if (configured) {
        return configured.replace(/\/$/, '');
    }

    if (window.location.protocol === 'file:') {
        return 'http://localhost:8080/api';
    }

    if (window.location.hostname === 'localhost' && window.location.port === '3000') {
        return 'http://localhost:8080/api';
    }

    return `${window.location.origin}/api`;
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
    const initialEl = document.getElementById('userInitial');
    if (initialEl) initialEl.textContent = userInitial;
    const nameEl = document.getElementById('userName');
    if (nameEl) nameEl.textContent = username || 'Nhan vien';
}

function setupActions() {
    const searchInput = document.getElementById('promoSearch');
    const typeFilter = document.getElementById('promoTypeFilter');
    const reloadBtn = document.getElementById('promoReloadBtn');

    searchInput?.addEventListener('input', (e) => {
        searchTerm = e.target.value || '';
        applyFilters();
    });

    typeFilter?.addEventListener('change', (e) => {
        filterType = e.target.value || 'ALL';
        applyFilters();
    });

    reloadBtn?.addEventListener('click', () => {
        loadPromotions();
    });
}

async function loadPromotions() {
    const grid = document.getElementById('promoGrid');
    const empty = document.getElementById('promoEmpty');
    if (grid) {
        grid.innerHTML = '<div class="promo-empty">Dang tai du lieu khuyen mai...</div>';
    }
    if (empty) empty.hidden = true;

    try {
        const headers = { 'Authorization': `Bearer ${sessionStorage.getItem('accessToken') || ''}` };
        const [promoRes, productRes] = await Promise.all([
            fetch(`${API_BASE}/promotions`, { headers }),
            fetch(`${API_BASE}/products`, { headers })
        ]);

        if (promoRes.status === 401 || productRes.status === 401) {
            sessionStorage.clear();
            window.location.href = '/pages/login.html';
            return;
        }

        if (!promoRes.ok || !productRes.ok) {
            throw new Error('Failed to load promotions');
        }

        const allPromotions = await promoRes.json();
        promotions = (allPromotions || []).filter(isPromotionActive);
        products = await productRes.json();
        promoProducts = buildPromoProducts(promotions, products);
        updateSummary();
        applyFilters();
    } catch (err) {
        if (grid) {
            grid.innerHTML = '';
        }
        if (empty) {
            empty.hidden = false;
            empty.textContent = 'Khong the tai danh sach khuyen mai.';
        }
    }
}

function buildPromoProducts(activePromos, productList) {
    const productMap = new Map((productList || []).map((p) => [p.id, p]));
    const results = new Map();

    (activePromos || []).forEach((promo) => {
        const targetIds = new Set();
        const targets = promo.targets || [];
        const bundles = promo.bundleItems || [];

        targets.forEach((target) => {
            if (!target || target.targetId == null) return;
            if (target.targetType === 'PRODUCT') {
                targetIds.add(target.targetId);
            }
            if (target.targetType === 'CATEGORY') {
                (productList || []).forEach((product) => {
                    if (product.categoryId === target.targetId) {
                        targetIds.add(product.id);
                    }
                });
            }
        });

        bundles.forEach((item) => {
            if (item?.productId != null) {
                targetIds.add(item.productId);
            }
        });

        targetIds.forEach((id) => {
            const product = productMap.get(id);
            if (!product) return;
            if (!results.has(id)) {
                results.set(id, { product, promotions: [] });
            }
            results.get(id).promotions.push(promo);
        });
    });

    return Array.from(results.values()).map((entry) => {
        const best = selectBestPromotion(entry.product, entry.promotions);
        return {
            product: entry.product,
            promotions: entry.promotions,
            bestPromotion: best.promo,
            promoPrice: best.price,
            promoLabel: best.label
        };
    });
}

function selectBestPromotion(product, promos) {
    const basePrice = Number(product?.price);
    const candidates = (promos || []).map((promo) => {
        const price = getPromoPrice(basePrice, promo);
        return { promo, price };
    });

    const priced = candidates.filter((c) => Number.isFinite(c.price));
    if (priced.length > 0) {
        priced.sort((a, b) => a.price - b.price);
        return { promo: priced[0].promo, price: priced[0].price, label: getDiscountLabel(priced[0].promo) };
    }

    const fallback = candidates[0]?.promo || null;
    return { promo: fallback, price: NaN, label: fallback ? getDiscountLabel(fallback) : '-' };
}

function getPromoPrice(basePrice, promo) {
    if (!Number.isFinite(basePrice) || !promo) return NaN;
    const value = Number(promo.discountValue);
    switch (promo.discountType) {
        case 'PERCENT':
            if (!Number.isFinite(value)) return NaN;
            return Math.max(0, basePrice * (1 - value / 100));
        case 'FIXED':
        case 'FIXED_AMOUNT':
            if (!Number.isFinite(value)) return NaN;
            return Math.max(0, basePrice - value);
        case 'BUNDLE':
            if (!Number.isFinite(value)) return NaN;
            return Math.max(0, value);
        case 'FREE_GIFT':
            return basePrice;
        default:
            return NaN;
    }
}

function getDiscountLabel(promo) {
    if (!promo) return '-';
    const value = Number(promo.discountValue);
    if (promo.discountType === 'PERCENT' && Number.isFinite(value)) {
        return `-${value}%`;
    }
    if ((promo.discountType === 'FIXED' || promo.discountType === 'FIXED_AMOUNT') && Number.isFinite(value)) {
        return `-${formatPrice(value)}`;
    }
    if (promo.discountType === 'BUNDLE') {
        return 'Combo';
    }
    if (promo.discountType === 'FREE_GIFT') {
        return 'Tang kem';
    }
    return promo.discountType || '-';
}

function applyFilters() {
    const grid = document.getElementById('promoGrid');
    const empty = document.getElementById('promoEmpty');
    if (!grid) return;

    const keyword = normalizeKeyword(searchTerm);
    const filtered = promoProducts.filter((entry) => {
        const matchesType = filterType === 'ALL' || entry.promotions.some((promo) => {
            if (promo.discountType === filterType) return true;
            if (filterType === 'FIXED' && promo.discountType === 'FIXED_AMOUNT') return true;
            if (filterType === 'BUNDLE' && promo.discountType === 'FREE_GIFT') return true;
            return false;
        });
        if (!matchesType) return false;
        if (!keyword) return true;

        const product = entry.product || {};
        const productText = `${product.name || ''} ${product.code || ''} ${product.barcode || ''}`;
        if (normalizeKeyword(productText).includes(keyword)) return true;

        return entry.promotions.some((promo) => {
            const promoText = `${promo.name || ''} ${promo.code || ''}`;
            return normalizeKeyword(promoText).includes(keyword);
        });
    });

    renderPromoGrid(filtered);
    if (empty) {
        empty.hidden = filtered.length > 0;
    }
}

function renderPromoGrid(list) {
    const grid = document.getElementById('promoGrid');
    if (!grid) return;

    if (!list || list.length === 0) {
        grid.innerHTML = '';
        return;
    }

    grid.innerHTML = list.map((entry) => {
        const product = entry.product || {};
        const promo = entry.bestPromotion || {};
        const basePrice = Number(product.price);
        const promoPrice = entry.promoPrice;
        const priceOld = Number.isFinite(basePrice) ? formatPrice(basePrice) : '-';
        const priceNew = Number.isFinite(promoPrice) ? formatPrice(promoPrice) : '-';
        const promoName = promo.name || promo.code || 'Khuyen mai';
        const promoCode = promo.code || '-';
        const promoType = promo.discountType || '-';
        const promoDates = formatPromoDates(promo.startDate, promo.endDate);

        return `
            <div class="promo-card">
                <div class="promo-header">
                    <div>
                        <div class="promo-title">${escapeHtml(product.name || 'San pham')}</div>
                        <div class="promo-sku">SKU: ${escapeHtml(product.code || product.barcode || '-')}</div>
                    </div>
                    <div class="promo-badge">${escapeHtml(entry.promoLabel)}</div>
                </div>
                <div class="promo-prices">
                    <span class="promo-price-old">${priceOld}</span>
                    <span class="promo-price-new">${priceNew}</span>
                </div>
                <div class="promo-meta">
                    <span>Khuyen mai <strong>${escapeHtml(promoName)}</strong></span>
                    <span>Ma <strong>${escapeHtml(promoCode)}</strong></span>
                    <span>Loai <strong>${escapeHtml(promoType)}</strong></span>
                    <span>Thoi gian <strong>${escapeHtml(promoDates)}</strong></span>
                </div>
            </div>
        `;
    }).join('');
}

function updateSummary() {
    const summary = document.getElementById('promoSummary');
    if (!summary) return;
    const promoCount = promotions.length;
    const productCount = promoProducts.length;
    summary.textContent = `${promoCount} khuyen mai dang ap dung | ${productCount} san pham giam gia`;
}

function formatPromoDates(startDate, endDate) {
    const start = formatDate(startDate);
    const end = formatDate(endDate);
    if (start === '-' && end === '-') return '-';
    return `${start} - ${end}`;
}

function formatDate(value) {
    if (!value) return '-';
    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value;
        const date = new Date(year, (month || 1) - 1, day || 1, hour, minute, second);
        if (!Number.isNaN(date.getTime())) {
            return date.toLocaleDateString('vi-VN');
        }
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '-';
    return date.toLocaleDateString('vi-VN');
}

function isPromotionActive(promo) {
    if (!promo) return false;
    if (promo.active === false) return false;
    const now = new Date();
    const start = parsePromotionDate(promo.startDate);
    const end = parsePromotionDate(promo.endDate);
    if (start && now < start) return false;
    if (end && now > end) return false;
    return true;
}

function parsePromotionDate(value) {
    if (!value) return null;
    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value;
        const date = new Date(year, (month || 1) - 1, day || 1, hour, minute, second);
        return Number.isNaN(date.getTime()) ? null : date;
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
}

function formatPrice(value) {
    const number = Number(value);
    if (!Number.isFinite(number)) return '-';
    return `${number.toLocaleString('vi-VN')}d`;
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
