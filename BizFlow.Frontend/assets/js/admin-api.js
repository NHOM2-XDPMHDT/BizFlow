/**
 * Admin API Client (v2)
 * Kết nối tới các Admin Microservices
 * Hỗ trợ fallback endpoints
 */

class AdminAPIClient {
    constructor() {
        const frontendEnv = (typeof window !== 'undefined' && window.__env) || {};
        const gatewayUrl = frontendEnv.API_GATEWAY_URL || 'http://localhost:8000';
        this.baseURL = gatewayUrl; // API Gateway (default docker-compose port)
        this.timeout = 5000;
        this.token = localStorage.getItem('authToken');
    }

    /**
     * Fetch dữ liệu người dùng (hỗ trợ pagination)
     */
    async getUsers(page = 0, size = 20) {
        try {
            // Cố gắng endpoint mới trước
            let response = await fetch(
                `${this.baseURL}/admin/users?page=${page}&size=${size}`,
                {
                    method: 'GET',
                    headers: this.getHeaders(),
                }
            );
            
            // Fallback endpoint cũ nếu 404
            if (response.status === 404) {
                response = await fetch(
                    `${this.baseURL}/admin/users`,
                    {
                        method: 'GET',
                        headers: this.getHeaders(),
                    }
                );
            }
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const data = await response.json();
            
            // Chuẩn hóa response
            return {
                success: true,
                data: data.data || data.users || data || [],
                total: data.total || (Array.isArray(data) ? data.length : 0),
                totalPages: data.totalPages || Math.ceil((data.total || (Array.isArray(data) ? data.length : 0)) / size),
                currentPage: page
            };
        } catch (error) {
            console.error('❌ Lỗi khi fetch người dùng:', error);
            return { 
                success: false, 
                error: error.message,
                data: [],
                total: 0,
                totalPages: 0
            };
        }
    }

    /**
     * Fetch dữ liệu sản phẩm
     */
    async getProducts(page = 0, size = 20) {
        try {
            let response = await fetch(
                `${this.baseURL}/admin/products?page=${page}&size=${size}`,
                {
                    method: 'GET',
                    headers: this.getHeaders(),
                }
            );
            
            if (response.status === 404) {
                response = await fetch(
                    `${this.baseURL}/admin/products`,
                    {
                        method: 'GET',
                        headers: this.getHeaders(),
                    }
                );
            }
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const data = await response.json();
            
            return {
                success: true,
                data: data.data || data.products || data || [],
                total: data.total || (Array.isArray(data) ? data.length : 0),
                totalPages: data.totalPages || Math.ceil((data.total || (Array.isArray(data) ? data.length : 0)) / size),
                currentPage: page
            };
        } catch (error) {
            console.error('❌ Lỗi khi fetch sản phẩm:', error);
            return { 
                success: false, 
                error: error.message,
                data: [],
                total: 0,
                totalPages: 0
            };
        }
    }

    /**
     * Fetch dữ liệu đơn hàng
     */
    async getOrders(page = 0, size = 20) {
        try {
            let response = await fetch(
                `${this.baseURL}/admin/orders?page=${page}&size=${size}`,
                {
                    method: 'GET',
                    headers: this.getHeaders(),
                }
            );
            
            if (response.status === 404) {
                response = await fetch(
                    `${this.baseURL}/admin/orders`,
                    {
                        method: 'GET',
                        headers: this.getHeaders(),
                    }
                );
            }
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const data = await response.json();
            
            return {
                success: true,
                data: data.data || data.orders || data || [],
                total: data.total || (Array.isArray(data) ? data.length : 0),
                totalPages: data.totalPages || Math.ceil((data.total || (Array.isArray(data) ? data.length : 0)) / size),
                currentPage: page
            };
        } catch (error) {
            console.error('❌ Lỗi khi fetch đơn hàng:', error);
            return { 
                success: false, 
                error: error.message,
                data: [],
                total: 0,
                totalPages: 0
            };
        }
    }

    /**
     * Fetch dữ liệu báo cáo
     */
    async getReports(page = 0, size = 20) {
        try {
            let response = await fetch(
                `${this.baseURL}/admin/reports?page=${page}&size=${size}`,
                {
                    method: 'GET',
                    headers: this.getHeaders(),
                }
            );
            
            if (response.status === 404) {
                response = await fetch(
                    `${this.baseURL}/admin/reports`,
                    {
                        method: 'GET',
                        headers: this.getHeaders(),
                    }
                );
            }
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const data = await response.json();
            
            return {
                success: true,
                data: data.data || data.reports || data || [],
                total: data.total || (Array.isArray(data) ? data.length : 0),
                totalPages: data.totalPages || Math.ceil((data.total || (Array.isArray(data) ? data.length : 0)) / size),
                currentPage: page
            };
        } catch (error) {
            console.error('❌ Lỗi khi fetch báo cáo:', error);
            return { 
                success: false, 
                error: error.message,
                data: [],
                total: 0,
                totalPages: 0
            };
        }
    }

    /**
     * Cập nhật người dùng
     */
    async updateUser(userId, userData) {
        try {
            const response = await fetch(
                `${this.baseURL}/admin/users/${userId}`,
                {
                    method: 'PUT',
                    headers: this.getHeaders(),
                    body: JSON.stringify(userData),
                }
            );
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            return { success: true, data: await response.json() };
        } catch (error) {
            console.error('❌ Lỗi khi cập nhật người dùng:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Xóa người dùng
     */
    async deleteUser(userId) {
        try {
            const response = await fetch(
                `${this.baseURL}/admin/users/${userId}`,
                {
                    method: 'DELETE',
                    headers: this.getHeaders(),
                }
            );
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            return { success: true };
        } catch (error) {
            console.error('❌ Lỗi khi xóa người dùng:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Cập nhật sản phẩm
     */
    async updateProduct(productId, productData) {
        try {
            const response = await fetch(
                `${this.baseURL}/admin/products/${productId}`,
                {
                    method: 'PUT',
                    headers: this.getHeaders(),
                    body: JSON.stringify(productData),
                }
            );
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            return { success: true, data: await response.json() };
        } catch (error) {
            console.error('❌ Lỗi khi cập nhật sản phẩm:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Xóa sản phẩm
     */
    async deleteProduct(productId) {
        try {
            const response = await fetch(
                `${this.baseURL}/admin/products/${productId}`,
                {
                    method: 'DELETE',
                    headers: this.getHeaders(),
                }
            );
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            return { success: true };
        } catch (error) {
            console.error('❌ Lỗi khi xóa sản phẩm:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Cập nhật trạng thái đơn hàng
     */
    async updateOrderStatus(orderId, status) {
        try {
            const response = await fetch(
                `${this.baseURL}/admin/orders/${orderId}/status`,
                {
                    method: 'PUT',
                    headers: this.getHeaders(),
                    body: JSON.stringify({ status }),
                }
            );
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            return { success: true, data: await response.json() };
        } catch (error) {
            console.error('❌ Lỗi khi cập nhật trạng thái đơn hàng:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tìm kiếm người dùng
     */
    async searchUsers(keyword, page = 0, size = 20) {
        try {
            const response = await fetch(
                `${this.baseURL}/admin/users/search?q=${encodeURIComponent(keyword)}&page=${page}&size=${size}`,
                {
                    method: 'GET',
                    headers: this.getHeaders(),
                }
            );
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const data = await response.json();
            return {
                success: true,
                data: data.data || data.users || [],
                total: data.total || 0,
                totalPages: data.totalPages || 0
            };
        } catch (error) {
            console.error('❌ Lỗi khi tìm kiếm người dùng:', error);
            return { 
                success: false, 
                error: error.message,
                data: [],
                total: 0,
                totalPages: 0
            };
        }
    }

    /**
     * Tìm kiếm sản phẩm
     */
    async searchProducts(keyword, page = 0, size = 20) {
        try {
            const response = await fetch(
                `${this.baseURL}/admin/products/search?q=${encodeURIComponent(keyword)}&page=${page}&size=${size}`,
                {
                    method: 'GET',
                    headers: this.getHeaders(),
                }
            );
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const data = await response.json();
            return {
                success: true,
                data: data.data || data.products || [],
                total: data.total || 0,
                totalPages: data.totalPages || 0
            };
        } catch (error) {
            console.error('❌ Lỗi khi tìm kiếm sản phẩm:', error);
            return { 
                success: false, 
                error: error.message,
                data: [],
                total: 0,
                totalPages: 0
            };
        }
    }

    /**
     * Set token
     */
    setToken(token) {
        this.token = token;
        if (token) {
            localStorage.setItem('authToken', token);
        }
    }

    /**
     * Clear token
     */
    clearToken() {
        this.token = null;
        localStorage.removeItem('authToken');
    }

    /**
     * Lấy headers
     */
    getHeaders() {
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        return headers;
    }
}

// Initialize
const adminAPI = new AdminAPIClient();
