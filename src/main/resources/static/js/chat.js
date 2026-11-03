/**
 * Connect Chat - Comprehensive Main Chat Controller
 * Manages chat state, real-time STOMP messaging, attachments, UI panels, and modals.
 */

const ChatApp = (() => {
    // Application State
    const state = {
        currentUser: null,
        conversations: [],
        activeConversationId: null,
        activeConversationDetail: null,
        messages: [],
        activeTab: 'chats', // 'chats' | 'contacts' | 'starred'
        replyingTo: null,
        editingMessageId: null,
        typingTimer: null,
        isTyping: false,
        selectedGroupMemberIds: new Set(),
        cachedGroupContacts: [],
        availableAddMembers: [],
        currentExpiry: { duration: 'none', label: 'No Expiry', expiresAt: null },
        countdownInterval: null,
        activeSummaryData: null
    };

    // DOM Elements Cache
    const el = {};

    async function init(currentUser) {
        state.currentUser = currentUser;

        // Verify and synchronize with tab's isolated session token
        const storedToken = sessionStorage.getItem('cc_auth_token');
        if (storedToken) {
            try {
                const meRes = await Api.users.getMe();
                if (meRes && meRes.data) {
                    state.currentUser = meRes.data;
                    sessionStorage.setItem('cc_user', JSON.stringify(meRes.data));
                }
            } catch (err) {
                console.warn('[AUTH] Session token verification failed:', err);
            }
        }

        if (!state.currentUser) {
            window.location.href = '/login';
            return;
        }

        cacheDom();
        bindEvents();
        initWebSocket();
        loadConversations();
        startCountdownTicker();
    }

    function cacheDom() {
        el.conversationsList = document.getElementById('conversationsList');
        el.chatArea = document.getElementById('chatArea');
        el.chatEmptyState = document.getElementById('chatEmptyState');
        el.chatWorkspaceArea = document.getElementById('chatWorkspaceArea');
        el.chatHeaderTitle = document.getElementById('chatHeaderTitle');
        el.chatHeaderStatus = document.getElementById('chatHeaderStatus');
        el.chatHeaderAvatar = document.getElementById('chatHeaderAvatar');
        el.messagesContainer = document.getElementById('messagesContainer');
        el.typingIndicatorBar = document.getElementById('typingIndicatorBar');
        el.messageInput = document.getElementById('messageInput');
        el.sendMessageBtn = document.getElementById('sendMessageBtn');
        el.attachFileBtn = document.getElementById('attachFileBtn');
        el.fileInput = document.getElementById('fileInput');
        el.replyBanner = document.getElementById('replyBanner');
        el.replySender = document.getElementById('replySender');
        el.replyText = document.getElementById('replyText');
        el.cancelReplyBtn = document.getElementById('cancelReplyBtn');
        el.emojiTrayBtn = document.getElementById('emojiTrayBtn');
        el.emojiTray = document.getElementById('emojiTray');
        el.searchInput = document.getElementById('searchInput');
        el.detailsPanel = document.getElementById('detailsPanel');
        el.toggleDetailsBtn = document.getElementById('toggleDetailsBtn');
        el.toastContainer = document.getElementById('toastContainer');
    }

    function bindEvents() {
        // Navigation buttons
        document.querySelectorAll('.nav-btn[data-tab]').forEach(btn => {
            btn.addEventListener('click', () => switchTab(btn.dataset.tab));
        });

        // Search in sidebar
        el.searchInput.addEventListener('input', (e) => filterSidebarItems(e.target.value));

        // Message Composer
        el.messageInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSendMessage();
            } else {
                handleUserTyping();
            }
        });

        el.sendMessageBtn.addEventListener('click', handleSendMessage);

        // Attachment button & file input
        el.attachFileBtn.addEventListener('click', () => el.fileInput.click());
        el.fileInput.addEventListener('change', handleFileUpload);

        // Reply cancel
        el.cancelReplyBtn.addEventListener('click', clearReplyState);

        // Emoji tray toggle & click
        el.emojiTrayBtn.addEventListener('click', () => {
            el.emojiTray.classList.toggle('hidden');
        });

        document.querySelectorAll('.emoji-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                el.messageInput.value += btn.textContent;
                el.messageInput.focus();
                el.emojiTray.classList.add('hidden');
            });
        });

        // Details Panel Toggle
        el.toggleDetailsBtn.addEventListener('click', toggleDetailsPanel);

        // Export Conversation Button
        const exportBtn = document.getElementById('exportChatBtn');
        if (exportBtn) {
            exportBtn.addEventListener('click', handleExportTranscript);
        }

        // Modals Open Triggers
        document.getElementById('newChatBtn')?.addEventListener('click', () => openModal('newChatModal'));
        document.getElementById('newGroupBtn')?.addEventListener('click', openNewGroupModal);
        document.getElementById('openAddMemberBtn')?.addEventListener('click', openAddMemberModal);
        document.getElementById('profileNavBtn')?.addEventListener('click', () => openProfileModal());

        // Modals Close Triggers
        document.querySelectorAll('.modal-close-btn, .modal-overlay').forEach(item => {
            item.addEventListener('click', (e) => {
                if (e.target.classList.contains('modal-overlay') || e.target.classList.contains('modal-close-btn')) {
                    closeAllModals();
                }
            });
        });

        // Search users inside New Chat modal
        const userSearchInput = document.getElementById('userSearchInput');
        if (userSearchInput) {
            userSearchInput.addEventListener('input', debounce((e) => searchUsersModal(e.target.value), 250));
        }

        // Live search contacts inside New Group modal
        const groupMemberSearchInput = document.getElementById('groupMemberSearchInput');
        if (groupMemberSearchInput) {
            groupMemberSearchInput.addEventListener('input', (e) => filterGroupMemberSelection(e.target.value));
        }

        // Live search contacts inside Add Member modal
        const addMemberSearchInput = document.getElementById('addMemberSearchInput');
        if (addMemberSearchInput) {
            addMemberSearchInput.addEventListener('input', (e) => filterAddMemberList(e.target.value));
        }

        // New Group form submit
        document.getElementById('createGroupForm')?.addEventListener('submit', handleCreateGroup);

        // Profile update form submit
        document.getElementById('profileForm')?.addEventListener('submit', handleUpdateProfile);

        // Password change form submit
        document.getElementById('passwordForm')?.addEventListener('submit', handleChangePassword);

        // --- Feature 1: Self-Destructing Messages Event Listeners ---
        const expiryToggleBtn = document.getElementById('expiryToggleBtn');
        const expiryMenu = document.getElementById('expiryMenu');
        if (expiryToggleBtn && expiryMenu) {
            expiryToggleBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                expiryMenu.classList.toggle('hidden');
            });

            document.addEventListener('click', (e) => {
                if (!expiryMenu.classList.contains('hidden') && !expiryMenu.contains(e.target) && !expiryToggleBtn.contains(e.target)) {
                    expiryMenu.classList.add('hidden');
                }
            });
        }

        document.querySelectorAll('.expiry-opt').forEach(opt => {
            opt.addEventListener('click', (e) => {
                e.stopPropagation();
                const exp = opt.dataset.expiry;
                if (exp === 'none') {
                    setExpiry('none', 'No Expiry', null);
                } else if (exp === '10s') {
                    setExpiry('10s', '10 Seconds', null);
                } else if (exp === '1m') {
                    setExpiry('1m', '1 Minute', null);
                } else if (exp === '1h') {
                    setExpiry('1h', '1 Hour', null);
                } else if (exp === 'custom') {
                    const customContainer = document.getElementById('customExpiryContainer');
                    if (customContainer) {
                        customContainer.classList.toggle('hidden');
                        if (!customContainer.classList.contains('hidden')) {
                            const now = new Date(Date.now() + 5000);
                            now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
                            const minVal = now.toISOString().slice(0, 16);
                            const input = document.getElementById('customExpiryInput');
                            if (input) {
                                input.min = minVal;
                                if (!input.value) input.value = minVal;
                            }
                        }
                    }
                }
            });
        });

        document.getElementById('applyCustomExpiryBtn')?.addEventListener('click', (e) => {
            e.stopPropagation();
            handleApplyCustomExpiry();
        });

        document.getElementById('clearExpiryBtn')?.addEventListener('click', clearExpiryState);

        // --- Feature 2: AI-Powered Smart Chat Summarizer Event Listeners ---
        document.getElementById('summarizeChatBtn')?.addEventListener('click', handleSummarizeChat);
        document.getElementById('detailsSummarizeBtn')?.addEventListener('click', handleSummarizeChat);
        document.getElementById('retrySummaryBtn')?.addEventListener('click', handleSummarizeChat);
        document.getElementById('copySummaryBtn')?.addEventListener('click', handleCopySummary);
    }

    // ----------------------------------------------------
    // WebSocket Setup
    // ----------------------------------------------------
    function initWebSocket() {
        if (state.currentUser) {
            ChatWebSocket.setCurrentUser(state.currentUser);
        }
        ChatWebSocket.connect({
            onConnect: () => {
                console.log('[CHAT] WebSocket connected');
                if (state.activeConversationId) {
                    ChatWebSocket.subscribeConversation(state.activeConversationId);
                }
            },
            onMessage: handleIncomingMessage,
            onTyping: handleIncomingTyping,
            onReadReceipt: handleIncomingReadReceipt,
            onPresence: handleIncomingPresence
        }, state.currentUser?.id);
    }

    // ----------------------------------------------------
    // Conversations Loading & Rendering
    // ----------------------------------------------------
    async function loadConversations() {
        try {
            const res = await Api.conversations.getAll();
            if (res && res.data) {
                state.conversations = res.data;
                renderConversationsList();
                if (!state.activeConversationId && state.conversations.length > 0) {
                    openConversation(state.conversations[0].id);
                }
            }
        } catch (err) {
            showToast('Failed to load conversations', 'error');
        }
    }

    function renderConversationsList() {
        el.conversationsList.innerHTML = '';

        if (!state.conversations || state.conversations.length === 0) {
            el.conversationsList.innerHTML = `
                <div style="padding: 24px; text-align: center; color: var(--text-muted); font-size: 13px;">
                    No conversations yet.<br>Click the <strong>+</strong> button to start messaging!
                </div>`;
            return;
        }

        state.conversations.forEach(c => {
            const item = document.createElement('div');
            item.className = `conversation-item ${c.id === state.activeConversationId ? 'active' : ''}`;
            item.dataset.id = c.id;

            const isDirect = c.type === 'DIRECT';
            const title = escapeHtml(c.title || 'Chat');
            const initial = title ? title.charAt(0).toUpperCase() : '?';

            // Online status for direct chat
            let statusDotHtml = '';
            if (isDirect && c.directParticipant) {
                const statusClass = (c.directParticipant.onlineStatus || 'OFFLINE').toLowerCase();
                statusDotHtml = `<span class="status-dot ${statusClass}"></span>`;
            }

            const avatarHtml = c.groupAvatarUrl 
                ? `<img src="${escapeHtml(c.groupAvatarUrl)}" alt="${title}">`
                : initial;

            let previewText = 'No messages yet';
            let previewTime = '';

            if (c.lastMessage) {
                previewText = escapeHtml(c.lastMessage.content || (c.lastMessage.attachments?.length ? '[Attachment]' : ''));
                previewTime = formatTime(c.lastMessage.createdAt);
            }

            const unreadBadgeHtml = c.unreadCount > 0 
                ? `<span class="unread-badge">${c.unreadCount}</span>` 
                : '';

            item.innerHTML = `
                <div class="avatar-wrapper">
                    <div class="avatar">${avatarHtml}</div>
                    ${statusDotHtml}
                </div>
                <div class="conversation-info">
                    <div class="conversation-top-row">
                        <span class="conversation-name">${title}</span>
                        <span class="conversation-time">${previewTime}</span>
                    </div>
                    <div class="conversation-bottom-row">
                        <span class="conversation-preview">${previewText}</span>
                        ${unreadBadgeHtml}
                    </div>
                </div>
            `;

            item.addEventListener('click', () => openConversation(c.id));
            el.conversationsList.appendChild(item);
        });
    }

    // ----------------------------------------------------
    // Open Conversation
    // ----------------------------------------------------
    async function openConversation(conversationId) {
        if (state.activeConversationId === conversationId && state.activeConversationDetail) {
            return;
        }

        state.activeConversationId = conversationId;
        clearReplyState();

        // Mark active item in list
        document.querySelectorAll('.conversation-item').forEach(item => {
            item.classList.toggle('active', item.dataset.id == conversationId);
        });

        // Switch empty state to workspace
        el.chatEmptyState.classList.add('hidden');
        el.chatWorkspaceArea.classList.remove('hidden');

        try {
            // 1. Fetch details
            const detailRes = await Api.conversations.getDetail(conversationId);
            state.activeConversationDetail = detailRes.data;
            renderChatHeader();

            // 2. Fetch messages
            const msgRes = await Api.conversations.getMessages(conversationId, 0, 100);
            state.messages = (msgRes.data && msgRes.data.content) ? msgRes.data.content.reverse() : [];
            renderMessages();

            // 3. Subscribe STOMP channel for this conversation
            ChatWebSocket.subscribeConversation(conversationId);

            // 4. Mark conversation as read
            await Api.conversations.markAsRead(conversationId);
            ChatWebSocket.sendReadReceipt(conversationId);

            // Update unread badge in memory
            const conv = state.conversations.find(c => c.id === conversationId);
            if (conv) {
                conv.unreadCount = 0;
                renderConversationsList();
            }

            // Render right details panel
            renderDetailsPanel();

        } catch (err) {
            showToast('Error opening conversation: ' + (err.message || 'Unknown error'), 'error');
        }
    }

    function renderChatHeader() {
        const detail = state.activeConversationDetail;
        if (!detail) return;

        const title = escapeHtml(detail.title || 'Chat');
        el.chatHeaderTitle.textContent = title;

        const initial = title ? title.charAt(0).toUpperCase() : '?';
        el.chatHeaderAvatar.innerHTML = detail.groupAvatarUrl 
            ? `<img src="${escapeHtml(detail.groupAvatarUrl)}" alt="${title}">`
            : initial;

        if (detail.type === 'DIRECT' && detail.directParticipant) {
            const status = detail.directParticipant.onlineStatus || 'OFFLINE';
            el.chatHeaderStatus.textContent = status.charAt(0) + status.slice(1).toLowerCase();
            el.chatHeaderStatus.className = `header-status ${status.toLowerCase()}`;
        } else {
            const memberCount = detail.members ? detail.members.length : 0;
            el.chatHeaderStatus.textContent = `${memberCount} members`;
            el.chatHeaderStatus.className = 'header-status';
        }
    }

    // ----------------------------------------------------
    // Messages Rendering
    // ----------------------------------------------------
    function renderMessages() {
        el.messagesContainer.innerHTML = '';

        if (!state.messages || state.messages.length === 0) {
            el.messagesContainer.innerHTML = `
                <div style="padding: 40px; text-align: center; color: var(--text-muted); font-size: 13px;">
                    This is the start of your message history with <strong>${escapeHtml(state.activeConversationDetail?.title || 'this conversation')}</strong>.
                </div>`;
            return;
        }

        let lastDateStr = null;
        const now = Date.now();
        const activeMessages = state.messages.filter(m => !m.expired && (!m.expiresAt || new Date(m.expiresAt).getTime() > now));

        activeMessages.forEach(m => {
            // Date Divider
            const msgDateStr = formatDate(m.createdAt);
            if (msgDateStr !== lastDateStr) {
                const divider = document.createElement('div');
                divider.className = 'date-divider';
                divider.innerHTML = `<span>${msgDateStr}</span>`;
                el.messagesContainer.appendChild(divider);
                lastDateStr = msgDateStr;
            }

            // Message Row
            const row = createMessageRow(m);
            el.messagesContainer.appendChild(row);
        });

        scrollToBottom();
    }

    function isSentByCurrentUser(sender) {
        if (!sender || !state.currentUser) return false;
        if (sender.id != null && state.currentUser.id != null) {
            if (Number(sender.id) === Number(state.currentUser.id)) return true;
        }
        if (sender.username && state.currentUser.username) {
            if (sender.username.toLowerCase() === state.currentUser.username.toLowerCase()) return true;
        }
        if (sender.email && state.currentUser.email) {
            if (sender.email.toLowerCase() === state.currentUser.email.toLowerCase()) return true;
        }
        return false;
    }

    function createMessageRow(m) {
        const isSentByMe = isSentByCurrentUser(m.sender);
        const isSystem = m.type === 'SYSTEM';

        const row = document.createElement('div');
        row.className = `message-row ${isSystem ? 'system' : (isSentByMe ? 'sent' : 'received')}`;
        row.dataset.id = m.id;

        if (isSystem) {
            row.innerHTML = `<div class="message-bubble system">${escapeHtml(m.content)}</div>`;
            return row;
        }

        // Sender name for group received messages
        let senderNameHtml = '';
        if (!isSentByMe && state.activeConversationDetail?.type === 'GROUP' && m.sender) {
            senderNameHtml = `<div class="message-sender-name">${escapeHtml(m.sender.displayName)}</div>`;
        }

        // Reply quote block
        let replyQuoteHtml = '';
        if (m.replyTo) {
            replyQuoteHtml = `
                <div class="message-reply-preview">
                    <span class="reply-sender">${escapeHtml(m.replyTo.sender?.displayName || 'User')}</span>
                    <span>${escapeHtml(m.replyTo.content || '[Attachment]')}</span>
                </div>`;
        }

        // Attachments: Image, Video, Audio, and All Document Types
        let attachmentHtml = '';
        if (m.attachments && m.attachments.length > 0) {
            m.attachments.forEach(att => {
                const mime = (att.contentType || '').toLowerCase();
                const name = (att.fileName || '').toLowerCase();
                const isImage = mime.startsWith('image/') || /\.(jpg|jpeg|png|gif|webp|bmp|svg)$/i.test(name);
                const isVideo = mime.startsWith('video/') || /\.(mp4|webm|mov|mkv|avi|wmv)$/i.test(name);
                const isAudio = mime.startsWith('audio/') || /\.(mp3|wav|ogg|m4a|aac)$/i.test(name);

                if (isImage) {
                    attachmentHtml += `<img src="${att.downloadUrl}" class="attachment-image-preview" onclick="window.open('${att.downloadUrl}')" alt="${escapeHtml(att.fileName)}">`;
                } else if (isVideo) {
                    attachmentHtml += `
                        <video controls class="attachment-video-preview" preload="metadata">
                            <source src="${att.downloadUrl}" type="${mime || 'video/mp4'}">
                            Your browser does not support the video tag.
                        </video>`;
                } else if (isAudio) {
                    attachmentHtml += `
                        <audio controls class="attachment-audio-preview" preload="metadata">
                            <source src="${att.downloadUrl}" type="${mime || 'audio/mpeg'}">
                            Your browser does not support audio playback.
                        </audio>`;
                } else {
                    let badgeClass = 'other';
                    let ext = name.split('.').pop() || 'FILE';
                    ext = ext.toUpperCase();

                    if (['PDF'].includes(ext)) badgeClass = 'pdf';
                    else if (['DOC', 'DOCX'].includes(ext)) badgeClass = 'doc';
                    else if (['XLS', 'XLSX', 'CSV'].includes(ext)) badgeClass = 'xls';
                    else if (['PPT', 'PPTX'].includes(ext)) badgeClass = 'ppt';
                    else if (['ZIP', 'RAR', '7Z', 'TAR', 'GZ'].includes(ext)) badgeClass = 'zip';
                    else if (['JAVA', 'JS', 'HTML', 'CSS', 'JSON', 'SQL', 'PY', 'TXT', 'MD'].includes(ext)) badgeClass = 'code';

                    attachmentHtml += `
                        <div class="message-attachment-card">
                            <div class="attachment-icon">
                                <span class="attachment-badge ${badgeClass}">${escapeHtml(ext)}</span>
                            </div>
                            <div class="attachment-details">
                                <div class="attachment-name" title="${escapeHtml(att.fileName)}">${escapeHtml(att.fileName)}</div>
                                <div class="attachment-size">
                                    <span>${formatBytes(att.fileSize)}</span>
                                </div>
                            </div>
                            <a href="${att.downloadUrl}" target="_blank" download="${escapeHtml(att.fileName)}" class="attachment-download-btn" title="Download ${escapeHtml(att.fileName)}">
                                <svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" width="16" height="16">
                                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                    <polyline points="7 10 12 15 17 10"></polyline>
                                    <line x1="12" y1="15" x2="12" y2="3"></line>
                                </svg>
                            </a>
                        </div>`;
                }
            });
        }

        // Status receipt checkmark:
        // SENT: Single tick (✓)
        // DELIVERED: Double tick (✓✓)
        // READ: Double tick in vibrant teal (✓✓)
        let receiptHtml = '';
        if (isSentByMe) {
            if (m.status === 'READ') {
                receiptHtml = `
                    <span class="message-receipt-icon read" title="Read">
                        <svg fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24">
                            <path d="M18 6L7 17l-5-5"></path>
                            <path d="M22 10l-7.5 7.5-2-2"></path>
                        </svg>
                    </span>`;
            } else if (m.status === 'DELIVERED') {
                receiptHtml = `
                    <span class="message-receipt-icon delivered" title="Delivered">
                        <svg fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24">
                            <path d="M18 6L7 17l-5-5"></path>
                            <path d="M22 10l-7.5 7.5-2-2"></path>
                        </svg>
                    </span>`;
            } else {
                receiptHtml = `
                    <span class="message-receipt-icon sent" title="Sent">
                        <svg fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" viewBox="0 0 24 24">
                            <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                    </span>`;
            }
        }

        const editedLabel = m.edited ? '<span style="font-size:10px; opacity:0.8;">(edited)</span>' : '';
        const contentHtml = m.content ? `<div>${escapeHtml(m.content)}</div>` : '';

        // Self-destructing expiry indicator
        const isExpiring = m.expiresAt && !m.expired;
        let expiryBadgeHtml = '';
        if (isExpiring) {
            const remainingMs = new Date(m.expiresAt).getTime() - Date.now();
            if (remainingMs > 0) {
                const isWarn = remainingMs <= 10000;
                expiryBadgeHtml = `
                    <span class="message-expiry-badge ${isWarn ? 'warning' : ''}" data-expires="${m.expiresAt}" data-id="${m.id}" title="Self-destruct timer">
                        <span>⏱️</span>
                        <span class="countdown-text">${formatCountdown(remainingMs)}</span>
                    </span>`;
            }
        }

        row.innerHTML = `
            <div class="message-bubble-wrapper">
                ${senderNameHtml}
                <div class="message-bubble ${isSentByMe ? 'sent' : 'received'} ${isExpiring ? 'self-destructing' : ''}">
                    ${replyQuoteHtml}
                    ${attachmentHtml}
                    ${contentHtml}
                    <div class="message-meta">
                        ${editedLabel}
                        <span>${formatTime(m.createdAt)}</span>
                        ${expiryBadgeHtml}
                        ${receiptHtml}
                    </div>
                </div>
                <div class="message-actions-menu">
                    <button class="action-menu-btn" title="Reply" onclick="ChatApp.setReplyMessage(${m.id})">
                        <svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><polyline points="9 14 4 9 9 4"></polyline><path d="M20 20v-7a4 4 0 0 0-4-4H4"></path></svg>
                    </button>
                    <button class="action-menu-btn" title="${m.starred ? 'Unstar' : 'Star'}" onclick="ChatApp.handleToggleStar(${m.id})">
                        <svg fill="${m.starred ? '#E0983A' : 'none'}" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                    </button>
                    ${isSentByMe && !m.deleted ? `
                        <button class="action-menu-btn" title="Edit" onclick="ChatApp.promptEditMessage(${m.id})">
                            <svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                        </button>
                    ` : ''}
                    ${(isSentByMe || isGroupAdmin()) && !m.deleted ? `
                        <button class="action-menu-btn" title="Delete" onclick="ChatApp.handleDeleteMessage(${m.id})">
                            <svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                        </button>
                    ` : ''}
                </div>
            </div>
        `;

        return row;
    }

    // ----------------------------------------------------
    // Sending Messages
    // ----------------------------------------------------
    async function handleSendMessage() {
        const text = el.messageInput.value.trim();
        if (!text) return;
        if (!state.activeConversationId) {
            showToast('Please select or start a chat first', 'error');
            return;
        }

        const idempotencyKey = 'idemp_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8);
        const payload = {
            content: text,
            type: 'TEXT',
            replyToId: state.replyingTo ? state.replyingTo.id : null,
            idempotencyKey: idempotencyKey
        };

        // Attach self-destruct expiry if selected
        if (state.currentExpiry && state.currentExpiry.duration && state.currentExpiry.duration !== 'none') {
            payload.expiryDuration = state.currentExpiry.duration;
            if (state.currentExpiry.duration === 'custom' && state.currentExpiry.expiresAt) {
                payload.expiresAt = state.currentExpiry.expiresAt;
            }
        }

        const originalText = el.messageInput.value;
        // Optimistically clear input for smooth typing feel
        el.messageInput.value = '';
        clearReplyState();
        stopTyping();

        try {
            // Send via REST endpoint: guarantees DB transaction, links attachments, and triggers real-time STOMP broadcast
            const res = await Api.conversations.sendMessage(state.activeConversationId, payload);
            if (res && res.data) {
                handleIncomingMessage(res.data);
                // Clear ephemeral expiry state back to default
                clearExpiryState();
            } else {
                // If response is null or failed, restore the typed message so user never loses their text
                el.messageInput.value = originalText;
                showToast('Message not sent. Please try again.', 'error');
            }
        } catch (err) {
            // Restore original text so user can retry without losing their message
            el.messageInput.value = originalText;
            showToast('Failed to send message: ' + (err.message || 'Error'), 'error');
        }
    }

    // ----------------------------------------------------
    // Attachments Upload & Real-Time Sharing
    // ----------------------------------------------------
    async function handleFileUpload(e) {
        const file = e.target.files[0];
        if (!file || !state.activeConversationId) return;

        try {
            showToast(`Uploading ${file.name}...`, 'success');
            const res = await Api.attachments.upload(file);
            if (res && res.data) {
                // Send a message referencing the uploaded attachment ID
                const payload = {
                    content: '',
                    type: 'ATTACHMENT',
                    attachmentIds: [res.data.id],
                    idempotencyKey: 'att_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8)
                };

                // Attach self-destruct expiry if selected
                if (state.currentExpiry && state.currentExpiry.duration && state.currentExpiry.duration !== 'none') {
                    payload.expiryDuration = state.currentExpiry.duration;
                    if (state.currentExpiry.duration === 'custom' && state.currentExpiry.expiresAt) {
                        payload.expiresAt = state.currentExpiry.expiresAt;
                    }
                }

                // Send via REST to guarantee MySQL persistence and trigger STOMP broadcast to all active conversation subscribers
                const msgRes = await Api.conversations.sendMessage(state.activeConversationId, payload);
                if (msgRes && msgRes.data) {
                    handleIncomingMessage(msgRes.data);
                    clearExpiryState();
                }
                showToast('File sent to conversation!', 'success');
            }
        } catch (err) {
            showToast('Upload failed: ' + (err.message || 'File unsupported or exceeds 100MB'), 'error');
        } finally {
            el.fileInput.value = '';
        }
    }

    // ----------------------------------------------------
    // Typing Indicators
    // ----------------------------------------------------
    function handleUserTyping() {
        if (!state.activeConversationId) return;

        if (!state.isTyping) {
            state.isTyping = true;
            ChatWebSocket.sendTyping(state.activeConversationId, true);
        }

        clearTimeout(state.typingTimer);
        state.typingTimer = setTimeout(stopTyping, 2500);
    }

    function stopTyping() {
        if (state.isTyping && state.activeConversationId) {
            state.isTyping = false;
            ChatWebSocket.sendTyping(state.activeConversationId, false);
        }
    }

    // ----------------------------------------------------
    // Real-Time Incoming Handlers
    // ----------------------------------------------------
    function handleIncomingMessage(messageDto) {
        if (!messageDto) return;

        // Handle expired message notification from scheduler or real-time event
        if (messageDto.expired) {
            state.messages = state.messages.filter(m => m.id !== messageDto.id);
            if (el.messagesContainer) {
                const row = el.messagesContainer.querySelector(`.message-row[data-id="${messageDto.id}"]`);
                if (row) {
                    row.classList.add('disintegrating');
                    setTimeout(() => {
                        if (row && row.parentNode) {
                            row.remove();
                        }
                    }, 550);
                }
            }
            const conv = state.conversations.find(c => c.id === messageDto.conversationId);
            if (conv && conv.lastMessage && conv.lastMessage.id === messageDto.id) {
                conv.lastMessage = null;
                renderConversationsList();
            }
            return;
        }

        const isSentByMe = isSentByCurrentUser(messageDto.sender);

        // If message is for currently open conversation
        if (state.activeConversationId === messageDto.conversationId) {
            const existingIdx = state.messages.findIndex(m => m.id === messageDto.id);
            if (existingIdx !== -1) {
                // Message was updated (edit, delete, star, status change)
                state.messages[existingIdx] = messageDto;
            } else {
                state.messages.push(messageDto);
                // Send read receipt if received from someone else
                if (!isSentByMe) {
                    ChatWebSocket.sendReadReceipt(state.activeConversationId);
                }
            }
            renderMessages();
            if (messageDto.type === 'SYSTEM') {
                refreshActiveConversation();
            }
        }

        // Update conversation summary in list
        const conv = state.conversations.find(c => c.id === messageDto.conversationId);
        if (conv) {
            conv.lastMessage = messageDto;
            conv.lastMessageAt = messageDto.createdAt;
            if (state.activeConversationId !== messageDto.conversationId && !isSentByMe) {
                conv.unreadCount = (conv.unreadCount || 0) + 1;
            }
            // Move to top
            state.conversations = [conv, ...state.conversations.filter(c => c.id !== conv.id)];
            renderConversationsList();
        } else {
            // New conversation arrived, refresh list
            loadConversations();
        }
    }

    function handleIncomingTyping(typingDto) {
        if (!typingDto || typingDto.conversationId !== state.activeConversationId) return;
        if (isSentByCurrentUser(typingDto.user)) return;

        if (typingDto.typing) {
            el.typingIndicatorBar.textContent = `${typingDto.user?.displayName || 'Someone'} is typing...`;
        } else {
            el.typingIndicatorBar.textContent = '';
        }
    }

    function handleIncomingReadReceipt(receiptDto) {
        if (!receiptDto || receiptDto.conversationId !== state.activeConversationId) return;

        // Mark sent messages as READ
        let updated = false;
        state.messages.forEach(m => {
            if (isSentByCurrentUser(m.sender)) {
                if (m.status !== 'READ') {
                    m.status = 'READ';
                    updated = true;
                }
            }
        });
        if (updated) {
            renderMessages();
        }
    }

    function handleIncomingPresence(presenceDto) {
        if (!presenceDto) return;

        // Update in conversation list
        state.conversations.forEach(c => {
            if (c.directParticipant && c.directParticipant.id === presenceDto.userId) {
                c.directParticipant.onlineStatus = presenceDto.onlineStatus;
            }
        });
        renderConversationsList();

        // Update in header if currently open
        if (state.activeConversationDetail?.directParticipant?.id === presenceDto.userId) {
            state.activeConversationDetail.directParticipant.onlineStatus = presenceDto.onlineStatus;
            renderChatHeader();
        }
    }

    // ----------------------------------------------------
    // Message Actions (Reply, Edit, Delete, Star)
    // ----------------------------------------------------
    function setReplyMessage(messageId) {
        const msg = state.messages.find(m => m.id === messageId);
        if (!msg) return;

        state.replyingTo = msg;
        el.replySender.textContent = msg.sender?.displayName || 'User';
        el.replyText.textContent = msg.content || '[Attachment]';
        el.replyBanner.classList.remove('hidden');
        el.messageInput.focus();
    }

    function clearReplyState() {
        state.replyingTo = null;
        if (el.replyBanner) {
            el.replyBanner.classList.add('hidden');
        }
    }

    async function promptEditMessage(messageId) {
        const msg = state.messages.find(m => m.id === messageId);
        if (!msg) return;

        const newContent = prompt('Edit your message:', msg.content);
        if (newContent !== null && newContent.trim() !== '') {
            try {
                await Api.messages.edit(messageId, newContent.trim());
                showToast('Message updated', 'success');
            } catch (err) {
                showToast('Edit failed: ' + (err.message || 'Time limit exceeded'), 'error');
            }
        }
    }

    async function handleDeleteMessage(messageId) {
        if (!confirm('Are you sure you want to delete this message?')) return;

        try {
            await Api.messages.delete(messageId);
            showToast('Message deleted', 'success');
        } catch (err) {
            showToast('Delete failed: ' + (err.message || 'Unauthorized'), 'error');
        }
    }

    async function handleToggleStar(messageId) {
        try {
            await Api.messages.toggleStar(messageId);
            const msg = state.messages.find(m => m.id === messageId);
            if (msg) {
                msg.starred = !msg.starred;
                renderMessages();
            }
            showToast('Star updated', 'success');
        } catch (err) {
            showToast('Could not toggle star', 'error');
        }
    }

    // ----------------------------------------------------
    // Details Panel & Actions
    // ----------------------------------------------------
    function toggleDetailsPanel() {
        el.detailsPanel.classList.toggle('hidden');
    }

    function renderDetailsPanel() {
        const detail = state.activeConversationDetail;
        if (!detail) return;

        const isGroup = detail.type === 'GROUP';
        const title = escapeHtml(detail.title || 'Chat');

        document.getElementById('detailsName').textContent = title;
        document.getElementById('detailsBio').textContent = detail.description || '';
        document.getElementById('detailsAvatar').innerHTML = detail.groupAvatarUrl 
            ? `<img src="${escapeHtml(detail.groupAvatarUrl)}" alt="${title}">`
            : (title ? title.charAt(0).toUpperCase() : '?');

        // Contact info (mobile & email for direct chats)
        const phoneEl = document.getElementById('detailsPhone');
        const emailEl = document.getElementById('detailsEmail');
        if (!isGroup && detail.directParticipant) {
            if (phoneEl) phoneEl.textContent = detail.directParticipant.phoneNumber ? `📱 ${detail.directParticipant.phoneNumber}` : '';
            if (emailEl) emailEl.textContent = detail.directParticipant.email ? `✉️ ${detail.directParticipant.email}` : '';
        } else {
            if (phoneEl) phoneEl.textContent = '';
            if (emailEl) emailEl.textContent = '';
        }

        // Render Group Members section
        const membersSection = document.getElementById('detailsMembersSection');
        const membersList = document.getElementById('detailsMembersList');
        const memberCountEl = document.getElementById('detailsMemberCount');
        const actionButtonsContainer = document.getElementById('groupActionButtons');

        if (isGroup && detail.members) {
            membersSection.classList.remove('hidden');
            if (memberCountEl) memberCountEl.textContent = detail.members.length;
            membersList.innerHTML = '';

            const myMembership = detail.members.find(m => m.user && m.user.id === state.currentUser.id);
            const isOwner = myMembership && myMembership.role === 'OWNER';
            const isAdmin = myMembership && (myMembership.role === 'OWNER' || myMembership.role === 'ADMIN');

            detail.members.forEach(m => {
                const item = document.createElement('div');
                item.className = 'member-list-item';
                const isMemOwner = m.role === 'OWNER';
                const initial = m.user.displayName ? m.user.displayName.charAt(0).toUpperCase() : '?';
                const phone = m.user.phoneNumber ? ` • ${escapeHtml(m.user.phoneNumber)}` : '';
                const email = m.user.email ? escapeHtml(m.user.email) : '';

                // Can remove if current user is owner/admin, member is not owner, and member is not self
                let removeBtnHtml = '';
                if (isAdmin && !isMemOwner && m.user.id !== state.currentUser.id) {
                    removeBtnHtml = `
                        <button class="remove-member-btn" title="Remove ${escapeHtml(m.user.displayName)}">
                            &times;
                        </button>
                    `;
                }

                item.innerHTML = `
                    <div class="member-info" style="flex: 1; min-width: 0;">
                        <div class="avatar" style="width: 32px; height: 32px; font-size: 12px; flex-shrink: 0;">
                            ${initial}
                        </div>
                        <div style="flex: 1; min-width: 0;">
                            <div style="font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                                ${escapeHtml(m.user.displayName)}${m.user.id === state.currentUser.id ? ' <span style="font-size: 11px; color: var(--primary-teal);">(You)</span>' : ''}
                            </div>
                            <div style="font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                                ${email}${phone}
                            </div>
                        </div>
                    </div>
                    <div style="display: flex; align-items: center; gap: 6px;">
                        <span class="member-role-badge ${isMemOwner ? 'owner' : ''}">${m.role}</span>
                        ${removeBtnHtml}
                    </div>
                `;

                const removeBtn = item.querySelector('.remove-member-btn');
                if (removeBtn) {
                    removeBtn.addEventListener('click', (e) => {
                        e.stopPropagation();
                        handleRemoveGroupMember(detail.id, m.user.id, m.user.displayName);
                    });
                }

                membersList.appendChild(item);
            });

            // Action Buttons (Leave or Delete Group)
            if (actionButtonsContainer) {
                actionButtonsContainer.innerHTML = '';
                if (isOwner) {
                    const deleteBtn = document.createElement('button');
                    deleteBtn.type = 'button';
                    deleteBtn.className = 'btn-secondary';
                    deleteBtn.style.cssText = 'color: var(--status-danger); border-color: rgba(239, 68, 68, 0.3); font-size: 12px; padding: 6px 12px; cursor: pointer;';
                    deleteBtn.textContent = 'Delete Group';
                    deleteBtn.addEventListener('click', () => handleDeleteGroup(detail.id, detail.title));
                    actionButtonsContainer.appendChild(deleteBtn);
                } else {
                    const leaveBtn = document.createElement('button');
                    leaveBtn.type = 'button';
                    leaveBtn.className = 'btn-secondary';
                    leaveBtn.style.cssText = 'color: var(--status-danger); border-color: rgba(239, 68, 68, 0.3); font-size: 12px; padding: 6px 12px; cursor: pointer;';
                    leaveBtn.textContent = 'Leave Group';
                    leaveBtn.addEventListener('click', () => handleLeaveGroup(detail.id, detail.title));
                    actionButtonsContainer.appendChild(leaveBtn);
                }
            }
        } else {
            membersSection.classList.add('hidden');
            if (actionButtonsContainer) actionButtonsContainer.innerHTML = '';
        }
    }

    function handleExportTranscript() {
        if (!state.activeConversationId) return;
        window.open(Api.conversations.getExportUrl(state.activeConversationId), '_blank');
    }

    // ----------------------------------------------------
    // Modals: New Chat & New Group
    // ----------------------------------------------------
    async function searchUsersModal(query) {
        const listEl = document.getElementById('userSearchResults');
        listEl.innerHTML = '<div style="padding: 12px; color: var(--text-muted); font-size: 13px;">Searching...</div>';

        try {
            const res = await Api.users.search(query);
            listEl.innerHTML = '';

            if (!res.data || res.data.length === 0) {
                listEl.innerHTML = '<div style="padding: 12px; color: var(--text-muted); font-size: 13px;">No contacts found matching "' + escapeHtml(query) + '".</div>';
                return;
            }

            res.data.forEach(u => {
                const item = document.createElement('div');
                item.className = 'conversation-item';
                const initial = u.displayName ? u.displayName.charAt(0).toUpperCase() : '?';
                const phoneStr = u.phoneNumber ? escapeHtml(u.phoneNumber) : 'No mobile';
                const emailStr = u.email ? escapeHtml(u.email) : '';

                item.innerHTML = `
                    <div class="avatar-wrapper">
                        <div class="avatar">${initial}</div>
                    </div>
                    <div class="conversation-info" style="flex:1;">
                        <div class="conversation-name" style="font-weight:600;">${escapeHtml(u.displayName)}</div>
                        <div class="conversation-preview" style="font-size:12px; color:var(--dark-teal);">${phoneStr} &bull; ${emailStr}</div>
                    </div>
                    <button class="btn-teal" style="padding: 6px 14px; font-size: 12px; border-radius: var(--radius-sm); border:none; cursor:pointer;">Chat</button>
                `;
                item.querySelector('button').addEventListener('click', async () => {
                    closeAllModals();
                    try {
                        const directRes = await Api.conversations.createDirect({ recipientId: u.id });
                        if (directRes && directRes.data) {
                            await loadConversations();
                            openConversation(directRes.data.id);
                        }
                    } catch (err) {
                        showToast('Could not start chat: ' + (err.message || 'Error'), 'error');
                    }
                });
                listEl.appendChild(item);
            });
        } catch (err) {
            listEl.innerHTML = '<div style="padding: 12px; color: var(--status-danger); font-size: 13px;">Error searching users</div>';
        }
    }

    // ----------------------------------------------------
    // Dynamic Group Creation with Search & Multi-Member Selection
    // ----------------------------------------------------
    async function openNewGroupModal() {
        const titleEl = document.getElementById('groupTitle');
        const descEl = document.getElementById('groupDescription');
        const searchInput = document.getElementById('groupMemberSearchInput');
        if (titleEl) titleEl.value = '';
        if (descEl) descEl.value = '';
        if (searchInput) searchInput.value = '';

        state.selectedGroupMemberIds = new Set();
        updateGroupSelectedBadge();

        const listEl = document.getElementById('groupMembersSelectionList');
        if (listEl) {
            listEl.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--text-muted); font-size: 13px;">Loading contacts...</div>';
        }

        openModal('newGroupModal');

        try {
            const res = await Api.users.getContacts();
            const contacts = (res && res.data ? res.data : []).filter(u => u.id !== state.currentUser.id);
            state.cachedGroupContacts = contacts;
            renderGroupMemberSelectionList(contacts);
        } catch (err) {
            if (listEl) {
                listEl.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--status-danger); font-size: 13px;">Failed to load contacts</div>';
            }
        }
    }

    function renderGroupMemberSelectionList(contacts) {
        const listEl = document.getElementById('groupMembersSelectionList');
        if (!listEl) return;
        listEl.innerHTML = '';

        if (!contacts || contacts.length === 0) {
            listEl.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--text-muted); font-size: 13px;">No contacts found.</div>';
            return;
        }

        contacts.forEach(u => {
            const label = document.createElement('label');
            label.className = 'group-member-option';

            const isChecked = state.selectedGroupMemberIds.has(u.id);
            const initial = u.displayName ? u.displayName.charAt(0).toUpperCase() : '?';
            const phone = u.phoneNumber ? u.phoneNumber : '';
            const email = u.email || '';
            const contactSub = [phone, email].filter(Boolean).join(' • ');

            label.innerHTML = `
                <input type="checkbox" class="group-member-checkbox" value="${u.id}" ${isChecked ? 'checked' : ''}>
                <div class="avatar" style="width: 32px; height: 32px; font-size: 12px; flex-shrink: 0;">${initial}</div>
                <div style="flex: 1; min-width: 0;">
                    <div style="font-size: 13px; font-weight: 500; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtml(u.displayName)}</div>
                    <div style="font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtml(contactSub)}</div>
                </div>
            `;

            const checkbox = label.querySelector('input[type="checkbox"]');
            checkbox.addEventListener('change', (e) => {
                const uid = parseInt(e.target.value);
                if (e.target.checked) {
                    state.selectedGroupMemberIds.add(uid);
                } else {
                    state.selectedGroupMemberIds.delete(uid);
                }
                updateGroupSelectedBadge();
            });

            listEl.appendChild(label);
        });
    }

    function updateGroupSelectedBadge() {
        const badge = document.getElementById('groupSelectedCountBadge');
        if (badge) {
            const count = state.selectedGroupMemberIds.size;
            badge.textContent = `${count} selected`;
        }
    }

    function filterGroupMemberSelection(query) {
        const q = (query || '').trim().toLowerCase();
        if (!q) {
            renderGroupMemberSelectionList(state.cachedGroupContacts);
            return;
        }
        const filtered = state.cachedGroupContacts.filter(u => {
            const nameMatch = u.displayName && u.displayName.toLowerCase().includes(q);
            const emailMatch = u.email && u.email.toLowerCase().includes(q);
            const phoneMatch = u.phoneNumber && u.phoneNumber.toLowerCase().includes(q);
            return nameMatch || emailMatch || phoneMatch;
        });
        renderGroupMemberSelectionList(filtered);
    }

    async function handleCreateGroup(e) {
        e.preventDefault();
        const titleInput = document.getElementById('groupTitle');
        const descInput = document.getElementById('groupDescription');
        const submitBtn = document.getElementById('createGroupSubmitBtn');
        const title = titleInput.value.trim();
        const description = descInput ? descInput.value.trim() : '';
        const memberIds = Array.from(state.selectedGroupMemberIds);

        if (!title) {
            showToast('Group title is required', 'error');
            return;
        }

        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Creating...';
        }

        try {
            const res = await Api.groups.create({ title, description, memberIds });
            if (res && res.data) {
                closeAllModals();
                showToast(`Group "${title}" created!`, 'success');
                await loadConversations();
                openConversation(res.data.id);
            }
        } catch (err) {
            showToast('Group creation failed: ' + (err.message || 'Error'), 'error');
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Create Group';
            }
        }
    }

    // ----------------------------------------------------
    // In-Chat Add Member to Active Group
    // ----------------------------------------------------
    async function openAddMemberModal() {
        const detail = state.activeConversationDetail;
        if (!detail || detail.type !== 'GROUP') {
            showToast('Please open a group conversation first', 'error');
            return;
        }

        const searchInput = document.getElementById('addMemberSearchInput');
        if (searchInput) searchInput.value = '';

        const container = document.getElementById('addMemberListContainer');
        if (container) {
            container.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--text-muted); font-size: 13px;">Loading contacts...</div>';
        }

        openModal('addMemberModal');

        try {
            const res = await Api.users.getContacts();
            const allContacts = res && res.data ? res.data : [];
            const existingMemberIds = new Set((detail.members || []).map(m => m.user.id));

            state.availableAddMembers = allContacts.filter(u => u.id !== state.currentUser.id && !existingMemberIds.has(u.id));
            renderAddMemberList(state.availableAddMembers);
        } catch (err) {
            if (container) {
                container.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--status-danger); font-size: 13px;">Failed to load contacts</div>';
            }
        }
    }

    function renderAddMemberList(contacts) {
        const container = document.getElementById('addMemberListContainer');
        if (!container) return;
        container.innerHTML = '';

        if (!contacts || contacts.length === 0) {
            container.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--text-muted); font-size: 13px;">All contacts are already members of this group.</div>';
            return;
        }

        contacts.forEach(u => {
            const item = document.createElement('div');
            item.className = 'add-member-item';
            const initial = u.displayName ? u.displayName.charAt(0).toUpperCase() : '?';
            const contactSub = [u.phoneNumber, u.email].filter(Boolean).join(' • ');

            item.innerHTML = `
                <div style="display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0;">
                    <div class="avatar" style="width: 32px; height: 32px; font-size: 12px; flex-shrink: 0;">${initial}</div>
                    <div style="flex: 1; min-width: 0;">
                        <div style="font-size: 13px; font-weight: 500; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtml(u.displayName)}</div>
                        <div style="font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtml(contactSub)}</div>
                    </div>
                </div>
                <button type="button" class="btn-teal add-btn" style="font-size: 11px; padding: 4px 12px; border-radius: 12px; height: auto; cursor: pointer;">Add</button>
            `;

            const addBtn = item.querySelector('.add-btn');
            addBtn.addEventListener('click', async () => {
                addBtn.disabled = true;
                addBtn.textContent = 'Adding...';
                try {
                    await Api.groups.addMember(state.activeConversationId, u.id);
                    showToast(`Added ${u.displayName} to group!`, 'success');
                    state.availableAddMembers = state.availableAddMembers.filter(m => m.id !== u.id);
                    renderAddMemberList(state.availableAddMembers);
                    await refreshActiveConversation();
                } catch (err) {
                    addBtn.disabled = false;
                    addBtn.textContent = 'Add';
                    showToast('Failed to add member: ' + (err.message || 'Error'), 'error');
                }
            });

            container.appendChild(item);
        });
    }

    function filterAddMemberList(query) {
        const q = (query || '').trim().toLowerCase();
        if (!q) {
            renderAddMemberList(state.availableAddMembers);
            return;
        }
        const filtered = state.availableAddMembers.filter(u => {
            const nameMatch = u.displayName && u.displayName.toLowerCase().includes(q);
            const emailMatch = u.email && u.email.toLowerCase().includes(q);
            const phoneMatch = u.phoneNumber && u.phoneNumber.toLowerCase().includes(q);
            return nameMatch || emailMatch || phoneMatch;
        });
        renderAddMemberList(filtered);
    }

    async function handleRemoveGroupMember(groupId, memberUserId, memberName) {
        if (!confirm(`Remove ${memberName} from this group?`)) return;
        try {
            await Api.groups.removeMember(groupId, memberUserId);
            showToast(`Removed ${memberName} from group`, 'success');
            await refreshActiveConversation();
        } catch (err) {
            showToast('Failed to remove member: ' + (err.message || 'Error'), 'error');
        }
    }

    async function handleLeaveGroup(groupId, groupTitle) {
        if (!confirm(`Are you sure you want to leave "${groupTitle}"?`)) return;
        try {
            await Api.groups.leave(groupId);
            showToast(`Left "${groupTitle}"`, 'success');
            closeActiveConversation();
            await loadConversations();
        } catch (err) {
            showToast('Failed to leave group: ' + (err.message || 'Error'), 'error');
        }
    }

    async function handleDeleteGroup(groupId, groupTitle) {
        if (!confirm(`Are you sure you want to permanently delete "${groupTitle}"? All messages and membership will be lost.`)) return;
        try {
            await Api.groups.delete(groupId);
            showToast(`Deleted group "${groupTitle}"`, 'success');
            closeActiveConversation();
            await loadConversations();
        } catch (err) {
            showToast('Failed to delete group: ' + (err.message || 'Error'), 'error');
        }
    }

    async function refreshActiveConversation() {
        if (!state.activeConversationId) return;
        try {
            const res = await Api.conversations.getDetail(state.activeConversationId);
            if (res && res.data) {
                state.activeConversationDetail = res.data;
                renderChatHeader();
                renderDetailsPanel();
            }
        } catch (err) {
            console.warn('[CHAT] Failed to refresh active conversation', err);
        }
    }

    function closeActiveConversation() {
        state.activeConversationId = null;
        state.activeConversationDetail = null;
        state.messages = [];
        ChatWebSocket.unsubscribeConversation();
        el.chatWorkspaceArea.classList.add('hidden');
        el.chatEmptyState.classList.remove('hidden');
        el.detailsPanel.classList.add('hidden');
    }

    // ----------------------------------------------------
    // User Profile & Settings Modal
    // ----------------------------------------------------
    function openProfileModal() {
        document.getElementById('profileDisplayName').value = state.currentUser.displayName || '';
        let phoneVal = state.currentUser.phoneNumber || '';
        phoneVal = phoneVal.replace(/^\+91\s*/, '').replace(/[\s\-]/g, '');
        document.getElementById('profilePhoneNumber').value = phoneVal;
        document.getElementById('profileEmail').value = state.currentUser.email || '';
        document.getElementById('profileBio').value = state.currentUser.bio || '';
        document.getElementById('profileAvatarUrl').value = state.currentUser.avatarUrl || '';
        openModal('profileModal');
    }

    async function handleUpdateProfile(e) {
        e.preventDefault();
        const displayName = document.getElementById('profileDisplayName').value.trim();
        let phoneNumber = document.getElementById('profilePhoneNumber').value.trim();
        const bio = document.getElementById('profileBio').value.trim();
        const avatarUrl = document.getElementById('profileAvatarUrl').value.trim();

        if (phoneNumber) {
            phoneNumber = phoneNumber.replace(/[\s\-]/g, '');
            if (!phoneNumber.startsWith('+')) {
                phoneNumber = '+91 ' + phoneNumber;
            }
        }

        try {
            const res = await Api.users.updateProfile({ displayName, phoneNumber, bio, avatarUrl });
            if (res && res.data) {
                state.currentUser = res.data;
                closeAllModals();
                showToast('Profile updated successfully!', 'success');
            }
        } catch (err) {
            showToast('Profile update failed: ' + (err.message || 'Error'), 'error');
        }
    }

    async function handleChangePassword(e) {
        e.preventDefault();
        const currentPassword = document.getElementById('currentPassword').value;
        const newPassword = document.getElementById('newPassword').value;

        if (!currentPassword || !newPassword) {
            showToast('Please fill in both password fields', 'error');
            return;
        }

        const isStrong = newPassword.length >= 8 &&
                         /[A-Z]/.test(newPassword) &&
                         /[a-z]/.test(newPassword) &&
                         /[0-9]/.test(newPassword) &&
                         /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/.test(newPassword);

        if (!isStrong) {
            showToast('Password must be at least 8 characters and include uppercase, lowercase, number, and special character', 'error');
            return;
        }

        try {
            await Api.users.changePassword({ currentPassword, newPassword });
            document.getElementById('passwordForm').reset();
            showToast('Password changed successfully!', 'success');
        } catch (err) {
            showToast('Password change failed: ' + (err.message || 'Incorrect password'), 'error');
        }
    }

    // ----------------------------------------------------
    // Helpers & Utilities
    // ----------------------------------------------------
    function openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('hidden');
        }
    }

    function closeAllModals() {
        document.querySelectorAll('.modal-overlay').forEach(m => m.classList.add('hidden'));
    }

    function scrollToBottom() {
        el.messagesContainer.scrollTop = el.messagesContainer.scrollHeight;
    }

    function isGroupAdmin() {
        if (!state.activeConversationDetail || state.activeConversationDetail.type !== 'GROUP') return false;
        const myMember = state.activeConversationDetail.members?.find(m => isSentByCurrentUser(m.user));
        return myMember && (myMember.role === 'OWNER' || myMember.role === 'ADMIN');
    }

    function showToast(message, type = 'success') {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.textContent = message;
        el.toastContainer.appendChild(toast);
        setTimeout(() => toast.remove(), 4000);
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#32;');
    }

    function formatTime(isoStr) {
        if (!isoStr) return '';
        const d = new Date(isoStr);
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    function formatDate(isoStr) {
        if (!isoStr) return '';
        const d = new Date(isoStr);
        const today = new Date();
        if (d.toDateString() === today.toDateString()) return 'Today';
        return d.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
    }

    function formatBytes(bytes) {
        if (!bytes || bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    }

    function debounce(func, wait) {
        let timeout;
        return function(...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), wait);
        };
    }

    function filterSidebarItems(query) {
        const q = query.toLowerCase().trim();
        document.querySelectorAll('.conversation-item').forEach(item => {
            const text = item.textContent.toLowerCase();
            item.style.display = text.includes(q) ? 'flex' : 'none';
        });
    }

    // ----------------------------------------------------
    // Innovative Feature 1: Self-Destructing Messages
    // ----------------------------------------------------
    function setExpiry(duration, label, expiresAt = null) {
        state.currentExpiry = { duration, label, expiresAt };

        document.querySelectorAll('.expiry-opt').forEach(opt => {
            opt.classList.toggle('active', opt.dataset.expiry === duration);
        });

        const pill = document.getElementById('composerExpiryPill');
        const pillLabel = document.getElementById('composerExpiryLabel');

        if (duration === 'none') {
            if (pill) pill.classList.add('hidden');
        } else {
            if (pillLabel) pillLabel.textContent = label;
            if (pill) pill.classList.remove('hidden');
        }

        const menu = document.getElementById('expiryMenu');
        if (menu) menu.classList.add('hidden');
        const customContainer = document.getElementById('customExpiryContainer');
        if (customContainer) customContainer.classList.add('hidden');
    }

    function clearExpiryState() {
        setExpiry('none', 'No Expiry', null);
        const customInput = document.getElementById('customExpiryInput');
        if (customInput) customInput.value = '';
    }

    function handleApplyCustomExpiry() {
        const input = document.getElementById('customExpiryInput');
        if (!input || !input.value) {
            showToast('Please select a date and time', 'error');
            return;
        }

        const date = new Date(input.value);
        if (isNaN(date.getTime()) || date.getTime() <= Date.now() + 2000) {
            showToast('Please select a future time (at least 5 seconds from now)', 'error');
            return;
        }

        const isoStr = date.toISOString();
        const formatted = date.toLocaleDateString([], { month: 'short', day: 'numeric' }) + ' ' +
            date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        setExpiry('custom', `Custom (${formatted})`, isoStr);
        showToast('Self-destruct timer set for ' + formatted, 'success');
    }

    function formatCountdown(ms) {
        if (ms <= 0) return '0s';
        const totalSec = Math.ceil(ms / 1000);
        if (totalSec < 60) {
            return totalSec + 's';
        }
        const mins = Math.floor(totalSec / 60);
        const secs = totalSec % 60;
        if (mins < 60) {
            return `${mins}m ${secs > 0 ? secs + 's' : ''}`.trim();
        }
        const hours = Math.floor(mins / 60);
        const remMins = mins % 60;
        if (hours < 24) {
            return `${hours}h ${remMins > 0 ? remMins + 'm' : ''}`.trim();
        }
        const days = Math.floor(hours / 24);
        return `${days}d`;
    }

    function startCountdownTicker() {
        if (state.countdownInterval) {
            clearInterval(state.countdownInterval);
        }
        state.countdownInterval = setInterval(() => {
            const badges = document.querySelectorAll('.message-expiry-badge[data-expires]');
            if (!badges.length) return;

            const now = Date.now();
            badges.forEach(badge => {
                const expiresStr = badge.getAttribute('data-expires');
                const msgId = badge.getAttribute('data-id');
                if (!expiresStr) return;

                const expiryTime = new Date(expiresStr).getTime();
                const diff = expiryTime - now;

                if (diff <= 0) {
                    const row = badge.closest('.message-row');
                    if (row && !row.classList.contains('disintegrating')) {
                        row.classList.add('disintegrating');
                        setTimeout(() => {
                            if (row && row.parentNode) {
                                row.remove();
                            }
                            if (msgId) {
                                state.messages = state.messages.filter(m => String(m.id) !== String(msgId));
                            }
                        }, 550);
                    }
                } else {
                    const textEl = badge.querySelector('.countdown-text');
                    if (textEl) {
                        textEl.textContent = formatCountdown(diff);
                    }
                    if (diff <= 10000) {
                        badge.classList.add('warning');
                    } else {
                        badge.classList.remove('warning');
                    }
                }
            });
        }, 500);
    }

    // ----------------------------------------------------
    // Innovative Feature 2: AI Chat Summarizer
    // ----------------------------------------------------
    async function handleSummarizeChat() {
        if (!state.activeConversationId) {
            showToast('Please open a conversation to summarize', 'error');
            return;
        }

        openModal('chatSummaryModal');

        const loadingState = document.getElementById('summaryLoadingState');
        const errorState = document.getElementById('summaryErrorState');
        const contentState = document.getElementById('summaryContentState');
        const errorMsg = document.getElementById('summaryErrorMessage');
        const copyBtn = document.getElementById('copySummaryBtn');

        if (loadingState) loadingState.classList.remove('hidden');
        if (errorState) errorState.classList.add('hidden');
        if (contentState) contentState.classList.add('hidden');
        if (copyBtn) copyBtn.disabled = true;

        try {
            const res = await Api.conversations.summarize(state.activeConversationId);
            if (!res || !res.data) {
                throw new Error('No summary returned from server');
            }

            const data = res.data;
            state.activeSummaryData = data;

            // Overview
            const overviewEl = document.getElementById('summaryOverviewText');
            if (overviewEl) overviewEl.textContent = data.overview || 'No conversation content available to summarize.';

            // Topics
            const topicsContainer = document.getElementById('summaryTopicsList');
            if (topicsContainer) {
                if (data.topics && data.topics.length > 0) {
                    topicsContainer.innerHTML = data.topics.map(t => `<span class="topic-tag">#${escapeHtml(t)}</span>`).join('');
                } else {
                    topicsContainer.innerHTML = '<span style="color:var(--text-muted); font-size:12px;">No specific topics tagged.</span>';
                }
            }

            // Bullet Lists
            renderSummaryList('summaryDecisionsList', data.decisions, 'No key decisions noted.');
            renderSummaryList('summaryKeyInfoList', data.keyInformation, 'No specific information highlights noted.');
            renderSummaryList('summaryActionItemsList', data.actionItems, 'No pending action items or tasks.');
            renderSummaryList('summaryDeadlinesList', data.deadlines, 'No deadlines or critical dates mentioned.');

            // Footer Badge
            const modelBadge = document.getElementById('summaryModelBadge');
            if (modelBadge) {
                const count = data.messageCountAnalyzed || 0;
                modelBadge.textContent = `AI: ${data.aiModelUsed || 'Gemini'} • ${count} message${count === 1 ? '' : 's'} analyzed`;
            }

            if (loadingState) loadingState.classList.add('hidden');
            if (contentState) contentState.classList.remove('hidden');
            if (copyBtn) copyBtn.disabled = false;

        } catch (err) {
            console.error('[AI SUMMARY] Error:', err);
            if (loadingState) loadingState.classList.add('hidden');
            if (errorState) errorState.classList.remove('hidden');
            if (errorMsg) errorMsg.textContent = err.message || 'AI service temporarily unavailable. Please try again.';
        }
    }

    function renderSummaryList(containerId, items, emptyMessage) {
        const ul = document.getElementById(containerId);
        if (!ul) return;
        ul.innerHTML = '';

        if (!items || items.length === 0) {
            const li = document.createElement('li');
            li.style.color = 'var(--text-muted)';
            li.style.fontStyle = 'italic';
            li.textContent = emptyMessage;
            ul.appendChild(li);
            return;
        }

        items.forEach(item => {
            const li = document.createElement('li');
            li.textContent = item;
            ul.appendChild(li);
        });
    }

    function handleCopySummary() {
        const data = state.activeSummaryData;
        if (!data) {
            showToast('No summary available to copy', 'error');
            return;
        }

        const lines = [];
        lines.push('=== CONNECT CHAT AI SUMMARY ===');
        lines.push(`Generated: ${new Date().toLocaleString()}`);
        if (state.activeConversationDetail?.title) {
            lines.push(`Conversation: ${state.activeConversationDetail.title}`);
        }
        lines.push('');
        lines.push('📌 EXECUTIVE OVERVIEW');
        lines.push(data.overview || 'N/A');
        lines.push('');

        if (data.topics && data.topics.length > 0) {
            lines.push('💬 TOPICS');
            lines.push(data.topics.map(t => `#${t}`).join(' '));
            lines.push('');
        }

        if (data.decisions && data.decisions.length > 0) {
            lines.push('✅ DECISIONS MADE');
            data.decisions.forEach(d => lines.push(`- ${d}`));
            lines.push('');
        }

        if (data.keyInformation && data.keyInformation.length > 0) {
            lines.push('ℹ️ KEY INFORMATION');
            data.keyInformation.forEach(k => lines.push(`- ${k}`));
            lines.push('');
        }

        if (data.actionItems && data.actionItems.length > 0) {
            lines.push('📋 TASKS & ACTION ITEMS');
            data.actionItems.forEach(a => lines.push(`- ${a}`));
            lines.push('');
        }

        if (data.deadlines && data.deadlines.length > 0) {
            lines.push('⏰ DEADLINES & DATES');
            data.deadlines.forEach(dl => lines.push(`- ${dl}`));
            lines.push('');
        }

        const fullText = lines.join('\n');
        navigator.clipboard.writeText(fullText)
            .then(() => showToast('Summary copied to clipboard!', 'success'))
            .catch(() => showToast('Failed to copy summary to clipboard', 'error'));
    }

    return {
        init,
        setReplyMessage,
        promptEditMessage,
        handleDeleteMessage,
        handleToggleStar,
        handleSummarizeChat,
        clearExpiryState
    };
})();

