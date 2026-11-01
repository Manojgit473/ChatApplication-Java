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
        isTyping: false
    };

    // DOM Elements Cache
    const el = {};

    function init(currentUser) {
        state.currentUser = currentUser;
        cacheDom();
        bindEvents();
        initWebSocket();
        loadConversations();
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
        document.getElementById('newGroupBtn')?.addEventListener('click', () => openModal('newGroupModal'));
        document.getElementById('profileNavBtn')?.addEventListener('click', () => openProfileModal());
        document.getElementById('vivaDemoNavBtn')?.addEventListener('click', () => openModal('vivaDemoModal'));

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

        // New Group form submit
        document.getElementById('createGroupForm')?.addEventListener('submit', handleCreateGroup);

        // Profile update form submit
        document.getElementById('profileForm')?.addEventListener('submit', handleUpdateProfile);

        // Password change form submit
        document.getElementById('passwordForm')?.addEventListener('submit', handleChangePassword);

        // Syllabus Demo Tab Buttons & Run
        document.querySelectorAll('.demo-tab-btn').forEach(tab => {
            tab.addEventListener('click', () => switchDemoTab(tab.dataset.demo));
        });
        document.getElementById('runDemoBtn')?.addEventListener('click', handleRunSyllabusDemo);
    }

    // ----------------------------------------------------
    // WebSocket Setup
    // ----------------------------------------------------
    function initWebSocket() {
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
        });
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

        state.messages.forEach(m => {
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

    function createMessageRow(m) {
        const isSentByMe = m.sender && m.sender.id === state.currentUser.id;
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

        // Attachments
        let attachmentHtml = '';
        if (m.attachments && m.attachments.length > 0) {
            m.attachments.forEach(att => {
                const isImage = att.contentType && att.contentType.startsWith('image/');
                if (isImage) {
                    attachmentHtml += `<img src="${att.downloadUrl}" class="attachment-image-preview" onclick="window.open('${att.downloadUrl}')" alt="Attachment">`;
                } else {
                    attachmentHtml += `
                        <div class="message-attachment-card">
                            <div class="attachment-icon">
                                <svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" width="16" height="16">
                                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                    <polyline points="14 2 14 8 20 8"></polyline>
                                </svg>
                            </div>
                            <div class="attachment-details">
                                <div class="attachment-name">${escapeHtml(att.fileName)}</div>
                                <div class="attachment-size">${formatBytes(att.fileSize)}</div>
                            </div>
                            <a href="${att.downloadUrl}" target="_blank" class="attachment-download-btn" title="Download">
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

        // Status receipt checkmark
        let receiptHtml = '';
        if (isSentByMe) {
            const isRead = m.status === 'READ';
            receiptHtml = `
                <span class="message-receipt-icon" title="${m.status}">
                    <svg fill="none" stroke="${isRead ? '#347F78' : 'currentColor'}" stroke-width="2" viewBox="0 0 24 24">
                        <path d="M20 6L9 17l-5-5"></path>
                        ${isRead ? '<path d="M20 12L9 23l-5-5"></path>' : ''}
                    </svg>
                </span>`;
        }

        const editedLabel = m.edited ? '<span style="font-size:10px; opacity:0.8;">(edited)</span>' : '';

        row.innerHTML = `
            <div class="message-bubble-wrapper">
                ${senderNameHtml}
                <div class="message-bubble ${isSentByMe ? 'sent' : 'received'}">
                    ${replyQuoteHtml}
                    ${attachmentHtml}
                    <div>${escapeHtml(m.content || '')}</div>
                    <div class="message-meta">
                        ${editedLabel}
                        <span>${formatTime(m.createdAt)}</span>
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
        if (!text || !state.activeConversationId) return;

        const idempotencyKey = 'idemp_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8);
        const payload = {
            content: text,
            type: 'TEXT',
            replyToId: state.replyingTo ? state.replyingTo.id : null,
            idempotencyKey: idempotencyKey
        };

        // Clear composer state immediately for responsive feel
        el.messageInput.value = '';
        clearReplyState();
        stopTyping();

        // 1. Attempt WebSocket send
        const sentViaWs = ChatWebSocket.sendMessage(state.activeConversationId, payload);

        // 2. If WS is not connected, fallback to REST
        if (!sentViaWs) {
            try {
                const res = await Api.conversations.sendMessage(state.activeConversationId, payload);
                if (res && res.data) {
                    handleIncomingMessage(res.data);
                }
            } catch (err) {
                showToast('Failed to send message: ' + (err.message || 'Error'), 'error');
            }
        }
    }

    // ----------------------------------------------------
    // Attachments Upload
    // ----------------------------------------------------
    async function handleFileUpload(e) {
        const file = e.target.files[0];
        if (!file || !state.activeConversationId) return;

        try {
            showToast(`Uploading ${file.name}...`, 'success');
            const res = await Api.attachments.upload(file);
            if (res && res.data) {
                // Send a message referencing the attachment
                const payload = {
                    content: '',
                    type: 'ATTACHMENT',
                    idempotencyKey: 'att_' + Date.now()
                };
                await Api.conversations.sendMessage(state.activeConversationId, payload);
                showToast('Attachment uploaded and shared!', 'success');
            }
        } catch (err) {
            showToast('Upload failed: ' + (err.message || 'File unsupported or too large'), 'error');
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

        // If message is for currently open conversation
        if (state.activeConversationId === messageDto.conversationId) {
            const existingIdx = state.messages.findIndex(m => m.id === messageDto.id);
            if (existingIdx !== -1) {
                // Message was updated (edit, delete, star)
                state.messages[existingIdx] = messageDto;
            } else {
                state.messages.push(messageDto);
                // Send read receipt if received from someone else
                if (messageDto.sender?.id !== state.currentUser.id) {
                    ChatWebSocket.sendReadReceipt(state.activeConversationId);
                }
            }
            renderMessages();
        }

        // Update conversation summary in list
        const conv = state.conversations.find(c => c.id === messageDto.conversationId);
        if (conv) {
            conv.lastMessage = messageDto;
            conv.lastMessageAt = messageDto.createdAt;
            if (state.activeConversationId !== messageDto.conversationId && messageDto.sender?.id !== state.currentUser.id) {
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
        if (typingDto.user?.id === state.currentUser.id) return;

        if (typingDto.typing) {
            el.typingIndicatorBar.textContent = `${typingDto.user?.displayName || 'Someone'} is typing...`;
        } else {
            el.typingIndicatorBar.textContent = '';
        }
    }

    function handleIncomingReadReceipt(receiptDto) {
        if (!receiptDto || receiptDto.conversationId !== state.activeConversationId) return;

        // Mark sent messages as READ
        state.messages.forEach(m => {
            if (m.sender?.id === state.currentUser.id) {
                m.status = 'READ';
            }
        });
        renderMessages();
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

        // Render Group Members section
        const membersSection = document.getElementById('detailsMembersSection');
        const membersList = document.getElementById('detailsMembersList');

        if (isGroup) {
            membersSection.classList.remove('hidden');
            membersList.innerHTML = '';

            detail.members.forEach(m => {
                const item = document.createElement('div');
                item.className = 'member-list-item';
                const isOwner = m.role === 'OWNER';
                item.innerHTML = `
                    <div class="member-info">
                        <div class="avatar" style="width: 32px; height: 32px; font-size: 12px;">
                            ${escapeHtml(m.user.displayName.charAt(0))}
                        </div>
                        <div>
                            <div style="font-size: 13px; font-weight: 500;">${escapeHtml(m.user.displayName)}</div>
                            <div style="font-size: 11px; color: var(--text-muted);">@${escapeHtml(m.user.username)}</div>
                        </div>
                    </div>
                    <span class="member-role-badge ${isOwner ? 'owner' : ''}">${m.role}</span>
                `;
                membersList.appendChild(item);
            });
        } else {
            membersSection.classList.add('hidden');
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
                listEl.innerHTML = '<div style="padding: 12px; color: var(--text-muted); font-size: 13px;">No users found.</div>';
                return;
            }

            res.data.forEach(u => {
                const item = document.createElement('div');
                item.className = 'conversation-item';
                item.innerHTML = `
                    <div class="avatar-wrapper">
                        <div class="avatar">${escapeHtml(u.displayName.charAt(0))}</div>
                    </div>
                    <div class="conversation-info">
                        <div class="conversation-name">${escapeHtml(u.displayName)}</div>
                        <div class="conversation-preview">@${escapeHtml(u.username)}</div>
                    </div>
                    <button class="btn-teal" style="padding: 4px 12px; font-size: 12px;">Chat</button>
                `;
                item.querySelector('button').addEventListener('click', async () => {
                    closeAllModals();
                    const directRes = await Api.conversations.createDirect({ recipientId: u.id });
                    if (directRes && directRes.data) {
                        await loadConversations();
                        openConversation(directRes.data.id);
                    }
                });
                listEl.appendChild(item);
            });
        } catch (err) {
            listEl.innerHTML = '<div style="padding: 12px; color: var(--status-danger); font-size: 13px;">Error searching users</div>';
        }
    }

    async function handleCreateGroup(e) {
        e.preventDefault();
        const title = document.getElementById('groupTitle').value.trim();
        const description = document.getElementById('groupDescription').value.trim();
        const checkedMembers = Array.from(document.querySelectorAll('.group-member-checkbox:checked')).map(cb => parseInt(cb.value));

        if (!title) {
            showToast('Group title is required', 'error');
            return;
        }

        try {
            const res = await Api.groups.create({ title, description, memberIds: checkedMembers });
            if (res && res.data) {
                closeAllModals();
                showToast(`Group "${title}" created!`, 'success');
                await loadConversations();
                openConversation(res.data.id);
            }
        } catch (err) {
            showToast('Group creation failed: ' + (err.message || 'Error'), 'error');
        }
    }

    // ----------------------------------------------------
    // User Profile & Settings Modal
    // ----------------------------------------------------
    function openProfileModal() {
        document.getElementById('profileDisplayName').value = state.currentUser.displayName || '';
        document.getElementById('profileBio').value = state.currentUser.bio || '';
        document.getElementById('profileAvatarUrl').value = state.currentUser.avatarUrl || '';
        openModal('profileModal');
    }

    async function handleUpdateProfile(e) {
        e.preventDefault();
        const displayName = document.getElementById('profileDisplayName').value.trim();
        const bio = document.getElementById('profileBio').value.trim();
        const avatarUrl = document.getElementById('profileAvatarUrl').value.trim();

        try {
            const res = await Api.users.updateProfile({ displayName, bio, avatarUrl });
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

        try {
            await Api.users.changePassword({ currentPassword, newPassword });
            document.getElementById('passwordForm').reset();
            showToast('Password changed successfully!', 'success');
        } catch (err) {
            showToast('Password change failed: ' + (err.message || 'Incorrect password'), 'error');
        }
    }

    // ----------------------------------------------------
    // Syllabus Viva Demonstration Modal
    // ----------------------------------------------------
    let activeDemoTopic = 'hibernate';

    function switchDemoTab(demoKey) {
        activeDemoTopic = demoKey;
        document.querySelectorAll('.demo-tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.demo === demoKey);
        });
        document.getElementById('demoOutput').textContent = 'Click "Execute Live Demo" to run this syllabus demonstration on the server.';
    }

    async function handleRunSyllabusDemo() {
        const outputEl = document.getElementById('demoOutput');
        outputEl.textContent = 'Executing on Spring Boot server...';

        try {
            let res;
            if (activeDemoTopic === 'hibernate') {
                res = await Api.demo.runHibernateSession();
            } else if (activeDemoTopic === 'reflection') {
                res = await Api.demo.runReflection();
            } else if (activeDemoTopic === 'serialization') {
                res = await Api.demo.runSerialization();
            } else if (activeDemoTopic === 'generics') {
                res = await Api.demo.runGenericsStreams();
            }

            if (res && res.data) {
                outputEl.textContent = JSON.stringify(res.data, null, 2);
            }
        } catch (err) {
            outputEl.textContent = 'Execution failed: ' + (err.message || 'Error running demo');
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
        const myMember = state.activeConversationDetail.members?.find(m => m.user?.id === state.currentUser.id);
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

    return {
        init,
        setReplyMessage,
        promptEditMessage,
        handleDeleteMessage,
        handleToggleStar
    };
})();

