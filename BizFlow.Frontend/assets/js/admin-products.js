/**
 * Admin Products Management
 * Quản lý sản phẩm
 */

class AdminProductsManager {
    constructor() {
        this.api = adminAPI;
        this.currentPage = 0;
        this.pageSize = 20;
        this.totalPages = 0;
        this.products = [];
        this.isLoading = false;
    }

    /**
     * Khởi tạo
     */
    async init() {
        this.setupEventListeners();
        await this.loadProducts();
    }

    /**
     * Setup event listeners
     */
    setupEventListeners() {
        // Search
        const searchInput = document.querySelector('.search input');
        if (searchInput) {
            let searchTimeout;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(() => {
                    this.searchProducts(e.target.value);
                }, 500);
            });
        }

        // Pagination
        const prevBtn = document.getElementById('prevPage');
        const nextBtn = document.getElementById('nextPage');
        if (prevBtn) prevBtn.addEventListener('click', () => this.previousPage());
        if (nextBtn) nextBtn.addEventListener('click', () => this.nextPage());
    }

    /**
     * Load sản phẩm
     */
    async loadProducts() {
        if (this.isLoading) return;
        
        this.isLoading = true;
        this.showLoading();

        try {
            const result = await this.api.getProducts(this.currentPage, this.pageSize);
            
            if (result.success) {
                this.products = result.data || result.products || [];
                this.totalPages = result.totalPages || Math.ceil((result.total || 0) / this.pageSize);
                this.renderProducts();
                this.updatePagination();
            } else {
                this.showError(result.error || 'Không thể tải dữ liệu sản phẩm');
            }
        } catch (error) {
            this.showError('Lỗi kết nối: ' + error.message);
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    /**
     * Render danh sách sản phẩm
     */
    renderProducts() {
        const tbody = document.getElementById('productsBody');
        if (!tbody) return;

        if (this.products.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-box">
                        <div style="text-align: center; padding: 20px;">
                            <div style="font-size: 24px; margin-bottom: 10px;">📦</div>
                            <div>Chưa có sản phẩm nào</div>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = this.products.map((product, index) => `
            <tr>
                <td><strong>${this.currentPage * this.pageSize + index + 1}</strong></td>
                <td>
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div style="width: 40px; height: 40px; border-radius: 4px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); display: flex; align-items: center; justify-content: center; color: white; font-size: 20px;">
                            ${this.getProductIcon(product.category || 'OTHER')}
                        </div>
                        <div>
                            <div style="font-weight: 500;">${this.escapeHtml(product.name || 'N/A')}</div>
                            <div style="font-size: 12px; color: #666;">${this.escapeHtml(product.sku || 'No SKU')}</div>
                        </div>
                    </div>
                </td>
                <td>
                    <span style="background: #f0f0f0; padding: 4px 8px; border-radius: 4px; font-size: 12px;">
                        ${this.escapeHtml(product.category || 'N/A')}
                    </span>
                </td>
                <td>
                    <strong style="color: #2196F3; font-size: 14px;">
                        ${this.formatPrice(product.price || 0)}
                    </strong>
                </td>
                <td>
                    <div style="display: flex; align-items: center; gap: 5px;">
                        <span style="background: #e8f5e9; color: #2e7d32; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 500;">
                            ${product.stock || 0}
                        </span>
                        <span style="color: #999; font-size: 11px;">cái</span>
                    </div>
                </td>
                <td>
                    <div style="display: flex; gap: 8px;">
                        <button class="btn-small" onclick="productsManager.editProduct(${product.id})" title="Chỉnh sửa">
                            ✏️
                        </button>
                        <button class="btn-small btn-danger" onclick="productsManager.deleteProductConfirm(${product.id})" title="Xóa">
                            🗑️
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    /**
     * Tìm kiếm sản phẩm
     */
    async searchProducts(keyword) {
        if (!keyword.trim()) {
            this.currentPage = 0;
            await this.loadProducts();
            return;
        }

        this.isLoading = true;
        this.showLoading();

        try {
            const result = await this.api.searchProducts(keyword);
            
            if (result.success) {
                this.products = result.data || result.products || [];
                this.currentPage = 0;
                this.totalPages = 1;
                this.renderProducts();
                this.updatePagination();
            } else {
                this.showError(result.error || 'Tìm kiếm thất bại');
            }
        } catch (error) {
            this.showError('Lỗi tìm kiếm: ' + error.message);
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    /**
     * Edit sản phẩm
     */
    editProduct(productId) {
        const product = this.products.find(p => p.id === productId);
        if (!product) {
            alert('Không tìm thấy sản phẩm');
            return;
        }

        const newPrice = prompt('Giá sản phẩm:', product.price);
        if (!newPrice || isNaN(newPrice)) return;

        this.updateProduct(productId, { price: parseFloat(newPrice) });
    }

    /**
     * Update sản phẩm
     */
    async updateProduct(productId, productData) {
        if (confirm('Bạn có chắc muốn cập nhật sản phẩm này?')) {
            try {
                const result = await this.api.updateProduct(productId, productData);
                if (result.success) {
                    alert('✓ Cập nhật thành công');
                    await this.loadProducts();
                } else {
                    alert('✗ Cập nhật thất bại: ' + (result.error || 'Unknown error'));
                }
            } catch (error) {
                alert('✗ Lỗi: ' + error.message);
            }
        }
    }

    /**
     * Xóa sản phẩm (xác nhận)
     */
    deleteProductConfirm(productId) {
        const product = this.products.find(p => p.id === productId);
        if (!product) {
            alert('Không tìm thấy sản phẩm');
            return;
        }

        if (confirm(`Bạn có chắc muốn xóa sản phẩm "${product.name}"?\n\nHành động này không thể hoàn tác!`)) {
            this.deleteProduct(productId);
        }
    }

    /**
     * Xóa sản phẩm
     */
    async deleteProduct(productId) {
        try {
            const result = await this.api.deleteProduct(productId);
            if (result.success) {
                alert('✓ Xóa thành công');
                await this.loadProducts();
            } else {
                alert('✗ Xóa thất bại: ' + (result.error || 'Unknown error'));
            }
        } catch (error) {
            alert('✗ Lỗi: ' + error.message);
        }
    }

    /**
     * Next page
     */
    nextPage() {
        if (this.currentPage < this.totalPages - 1) {
            this.currentPage++;
            this.loadProducts();
        }
    }

    /**
     * Previous page
     */
    previousPage() {
        if (this.currentPage > 0) {
            this.currentPage--;
            this.loadProducts();
        }
    }

    /**
     * Update pagination UI
     */
    updatePagination() {
        const pageInfo = document.getElementById('pageInfo');
        if (pageInfo) {
            pageInfo.textContent = `Trang ${this.currentPage + 1} / ${this.totalPages}`;
        }

        const prevBtn = document.getElementById('prevPage');
        const nextBtn = document.getElementById('nextPage');
        
        if (prevBtn) prevBtn.disabled = this.currentPage === 0;
        if (nextBtn) nextBtn.disabled = this.currentPage >= this.totalPages - 1;
    }

    /**
     * Format giá
     */
    formatPrice(price) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND',
        }).format(price);
    }

    /**
     * Get product icon
     */
    getProductIcon(category) {
        const icons = {
            'ELECTRONICS': '💻',
            'CLOTHING': '👕',
            'FOOD': '🍔',
            'BEAUTY': '💄',
            'HOME': '🏠',
            'SPORTS': '⚽',
            'BOOKS': '📚',
            'TOYS': '🎮',
            'OTHER': '📦',
        };
        return icons[category] || '📦';
    }

    /**
     * Show loading
     */
    showLoading() {
        const tbody = document.getElementById('productsBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-box">
                        <div style="text-align: center; padding: 20px;">
                            <div style="display: inline-block; animation: spin 1s linear infinite;">⏳</div>
                            <div style="margin-top: 10px;">Đang tải...</div>
                        </div>
                    </td>
                </tr>
            `;
        }
    }

    /**
     * Hide loading
     */
    hideLoading() {
        // Implementation can be added if needed
    }

    /**
     * Show error
     */
    showError(message) {
        const tbody = document.getElementById('productsBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="empty-box">
                        <div style="text-align: center; padding: 20px; color: #d32f2f;">
                            <div style="font-size: 24px; margin-bottom: 10px;">⚠️</div>
                            <div>${this.escapeHtml(message)}</div>
                            <button onclick="productsManager.loadProducts()" style="margin-top: 10px; padding: 6px 12px; background: #2196F3; color: white; border: none; border-radius: 4px; cursor: pointer;">
                                Thử lại
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }
    }

    /**
     * Escape HTML
     */
    escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, m => map[m]);
    }
}

// Initialize
let productsManager;
window.addEventListener('DOMContentLoaded', () => {
    productsManager = new AdminProductsManager();
    productsManager.init();
});
