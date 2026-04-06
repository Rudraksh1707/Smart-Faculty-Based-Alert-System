const API_BASE = window.location.origin.includes('3000') ? 'http://localhost:8080/api' : window.location.origin + '/api';

const api = {
    // Utility for generic fetch with JSON
    async request(endpoint, options = {}) {
        const url = `${API_BASE}${endpoint}`;
        
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        const config = {
            ...options,
            headers
        };
        
        try {
            const response = await fetch(url, config);
            const contentType = response.headers.get("content-type");
            
            if (!response.ok) {
                const errorData = contentType && contentType.includes("application/json") 
                    ? await response.json() 
                    : await response.text();
                throw new Error(errorData.message || errorData || 'API Error');
            }

            if (contentType && contentType.includes("application/json")) {
                return await response.json();
            } else {
                return await response.text();
            }
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    },

    // Auth
    auth: {
        login: (credentials) => api.request('/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),
        facultyLogin: (credentials) => api.request('/auth/faculty-login', { method: 'POST', body: JSON.stringify(credentials) }),
        register: (data) => api.request('/auth/register', { method: 'POST', body: JSON.stringify(data) }),
        facultyPasswordChange: (data) => api.request('/faculty/change-password', { method: 'PUT', body: JSON.stringify(data) })
    },

    // Dashboard
    dashboard: {
        getStats: () => api.request('/dashboard/stats')
    },

    // Faculty
    faculty: {
        getAll: () => api.request('/faculty'),
        create: (data) => api.request('/faculty', { method: 'POST', body: JSON.stringify(data) }),
        update: (id, data) => api.request(`/faculty/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
        delete: (id) => api.request(`/faculty/${id}`, { method: 'DELETE' }),
        linkTelegram: (data) => api.request('/faculty/link-telegram', { method: 'POST', body: JSON.stringify(data) })
    },

    // Timetable
    timetable: {
        getAll: () => api.request('/timetable'),
        create: (data) => api.request('/timetable', { method: 'POST', body: JSON.stringify(data) }),
        delete: (id) => api.request(`/timetable/${id}`, { method: 'DELETE' }),
        markAbsent: (data) => api.request('/timetable/mark-absent', { method: 'POST', body: JSON.stringify(data) }),
        updateStatus: (id, status) => api.request(`/timetable/${id}/status`, { method: 'PUT', body: JSON.stringify({status}) }),
        clearAll: () => api.request('/timetable/all', { method: 'DELETE' }),
        upload: (file, startDate, endDate, facultyId = null) => {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('startDate', startDate);
            formData.append('endDate', endDate);
            if (facultyId) {
                formData.append('facultyId', facultyId);
            }
            
            return fetch(`${API_BASE}/timetable/upload`, {
                method: 'POST',
                body: formData
            }).then(async res => {
                if(!res.ok) throw new Error(await res.text());
                return res.text();
            });
        }
    },

    // Browser Notifications
    notifications: {
        async requestPermission() {
            if (!("Notification" in window)) return 'unsupported';
            return await Notification.requestPermission();
        },
        send(title, body) {
            if ("Notification" in window && Notification.permission === "granted") {
                new Notification(title, { body, icon: '/favicon.ico' });
            }
        }
    }
};
