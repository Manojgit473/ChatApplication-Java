/**
 * Connect Chat - REST API Client Wrapper
 * Handles JSON serialization, headers, error parsing, and session credentials.
 */

const Api = (() => {
    async function request(endpoint, options = {}) {
        const config = {
            headers: {
                'Accept': 'application/json',
                ...options.headers
            },
            ...options
        };

        // Don't set Content-Type if uploading FormData (browser handles boundary)
        if (options.body && !(options.body instanceof FormData) && !config.headers['Content-Type']) {
            config.headers['Content-Type'] = 'application/json';
        }

        try {
            const response = await fetch(endpoint, config);

            if (response.status === 401) {
                // If unauthorized and not on auth pages, redirect to login
                if (!window.location.pathname.includes('/login') && !window.location.pathname.includes('/register')) {
                    window.location.href = '/login';
                    return null;
                }
            }

            const contentType = response.headers.get('content-type');
            let data = null;

            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else if (response.status !== 204) {
                data = await response.text();
            }

            if (!response.ok) {
                const errorMessage = (data && (data.message || data.error)) || `HTTP ${response.status} Error`;
                const error = new Error(errorMessage);
                error.status = response.status;
                error.data = data;
                throw error;
            }

            return data;
        } catch (err) {
            console.error(`[API ERROR] ${options.method || 'GET'} ${endpoint}:`, err);
            throw err;
        }
    }

    return {
        auth: {
            register: (body) => request('/api/auth/register', { method: 'POST', body: JSON.stringify(body) }),
            login: (body) => request('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }),
            logout: () => request('/api/auth/logout', { method: 'POST' }),
            getMe: () => request('/api/auth/me')
        },
        users: {
            getMe: () => request('/api/users/me'),
            updateProfile: (body) => request('/api/users/me', { method: 'PATCH', body: JSON.stringify(body) }),
            changePassword: (body) => request('/api/users/me/password', { method: 'POST', body: JSON.stringify(body) }),
            search: (query) => request(`/api/users/search?q=${encodeURIComponent(query || '')}`),
            getContacts: () => request('/api/users/contacts'),
            getUser: (id) => request(`/api/users/${id}`),
            block: (id) => request(`/api/users/${id}/block`, { method: 'POST' }),
            unblock: (id) => request(`/api/users/${id}/block`, { method: 'DELETE' }),
            getBlocked: () => request('/api/users/me/blocked')
        },
        conversations: {
            getAll: () => request('/api/conversations'),
            createDirect: (body) => request('/api/conversations', { method: 'POST', body: JSON.stringify(body) }),
            getDetail: (id) => request(`/api/conversations/${id}`),
            getMessages: (id, page = 0, size = 50) => request(`/api/conversations/${id}/messages?page=${page}&size=${size}`),
            sendMessage: (id, body) => request(`/api/conversations/${id}/messages`, { method: 'POST', body: JSON.stringify(body) }),
            markAsRead: (id) => request(`/api/conversations/${id}/read`, { method: 'POST' }),
            setMute: (id, muted) => request(`/api/conversations/${id}/mute?muted=${muted}`, { method: 'POST' }),
            setArchive: (id, archived) => request(`/api/conversations/${id}/archive?archived=${archived}`, { method: 'POST' }),
            getExportUrl: (id) => `/api/conversations/${id}/export`
        },
        groups: {
            create: (body) => request('/api/groups', { method: 'POST', body: JSON.stringify(body) }),
            update: (id, body) => request(`/api/groups/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
            addMember: (id, userId) => request(`/api/groups/${id}/members`, { method: 'POST', body: JSON.stringify({ userId }) }),
            removeMember: (id, userId) => request(`/api/groups/${id}/members/${userId}`, { method: 'DELETE' }),
            leave: (id) => request(`/api/groups/${id}/leave`, { method: 'POST' }),
            delete: (id) => request(`/api/groups/${id}`, { method: 'DELETE' })
        },
        messages: {
            edit: (id, content) => request(`/api/messages/${id}`, { method: 'PATCH', body: JSON.stringify({ content }) }),
            delete: (id) => request(`/api/messages/${id}`, { method: 'DELETE' }),
            toggleStar: (id) => request(`/api/messages/${id}/star`, { method: 'POST' }),
            getStarred: () => request('/api/messages/starred'),
            search: (convId, keyword) => request(`/api/messages/search?conversationId=${convId}&keyword=${encodeURIComponent(keyword)}`)
        },
        attachments: {
            upload: (file) => {
                const formData = new FormData();
                formData.append('file', file);
                return request('/api/attachments/upload', { method: 'POST', body: formData });
            }
        },
        demo: {
            runHibernateSession: () => request('/api/demo/hibernate-session'),
            runReflection: () => request('/api/demo/reflection'),
            runSerialization: () => request('/api/demo/serialization'),
            runGenericsStreams: () => request('/api/demo/generics-streams')
        }
    };
})();

