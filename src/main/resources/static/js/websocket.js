/**
 * Connect Chat - STOMP & SockJS Real-Time WebSocket Client
 */

const ChatWebSocket = (() => {
    let stompClient = null;
    let isConnected = false;
    let pendingConversationId = null;
    let currentUserId = null;
    const activeSubscriptions = new Map();
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 10;

    // Callbacks map
    const handlers = {
        onMessage: null,
        onTyping: null,
        onReadReceipt: null,
        onPresence: null,
        onConnect: null,
        onDisconnect: null
    };

    function connect(callbacks = {}, userId = null) {
        Object.assign(handlers, callbacks);
        if (userId) {
            currentUserId = userId;
        }

        // Fallback: Use SockJS if available, otherwise native WebSocket
        const socket = typeof SockJS !== 'undefined' ? new SockJS('/ws') : new WebSocket(`ws://${window.location.host}/ws`);
        stompClient = Stomp.over(socket);

        // Disable verbose console logging in production
        stompClient.debug = () => {};

        const token = sessionStorage.getItem('cc_auth_token');
        const connectHeaders = token ? { 'X-Auth-Token': token, 'Authorization': 'Bearer ' + token } : {};

        stompClient.connect(connectHeaders, onConnected, onError);
    }

    function onConnected(frame) {
        isConnected = true;
        reconnectAttempts = 0;
        console.log('[WS] Connected to STOMP broker');

        // Subscribe to global presence channel
        subscribePresence();

        // Subscribe to personal user message queue
        if (currentUserId) {
            subscribeUserQueue(currentUserId);
        }

        // Resubscribe to pending or active conversation
        if (pendingConversationId) {
            subscribeConversation(pendingConversationId);
            pendingConversationId = null;
        }

        if (handlers.onConnect) {
            handlers.onConnect();
        }
    }

    function onError(error) {
        isConnected = false;
        console.warn('[WS] STOMP error or disconnect:', error);
        if (handlers.onDisconnect) {
            handlers.onDisconnect();
        }

        // Auto reconnect with backoff
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            const timeout = Math.min(30000, 1000 * Math.pow(1.5, reconnectAttempts));
            console.log(`[WS] Reconnecting in ${Math.round(timeout / 1000)}s...`);
            setTimeout(() => connect(handlers, currentUserId), timeout);
        }
    }

    function subscribePresence() {
        if (!stompClient || !isConnected) return;

        stompClient.subscribe('/topic/presence', (message) => {
            try {
                const presenceData = JSON.parse(message.body);
                if (handlers.onPresence) {
                    handlers.onPresence(presenceData);
                }
            } catch (e) {
                console.error('[WS] Error parsing presence message', e);
            }
        });
    }

    function subscribeUserQueue(userId) {
        if (!stompClient || !isConnected || !userId) return;

        stompClient.subscribe(`/topic/users/${userId}/messages`, (msg) => {
            try {
                const messageDto = JSON.parse(msg.body);
                if (handlers.onMessage) {
                    handlers.onMessage(messageDto);
                }
            } catch (e) {
                console.error('[WS] Error parsing user message', e);
            }
        });

        stompClient.subscribe(`/topic/users/${userId}/read`, (msg) => {
            try {
                const receiptDto = JSON.parse(msg.body);
                if (handlers.onReadReceipt) {
                    handlers.onReadReceipt(receiptDto);
                }
            } catch (e) {
                console.error('[WS] Error parsing user read receipt', e);
            }
        });
    }

    function subscribeConversation(conversationId) {
        if (!conversationId) return;

        if (!stompClient || !isConnected) {
            pendingConversationId = conversationId;
            return;
        }

        // Unsubscribe from previous conversation topics if needed
        unsubscribeConversation();

        // 1. Subscribe to Messages
        const msgSub = stompClient.subscribe(`/topic/conversations/${conversationId}`, (msg) => {
            try {
                const messageDto = JSON.parse(msg.body);
                if (handlers.onMessage) {
                    handlers.onMessage(messageDto);
                }
            } catch (e) {
                console.error('[WS] Error parsing incoming message', e);
            }
        });

        // 2. Subscribe to Typing Indicators
        const typingSub = stompClient.subscribe(`/topic/conversations/${conversationId}/typing`, (msg) => {
            try {
                const typingDto = JSON.parse(msg.body);
                if (handlers.onTyping) {
                    handlers.onTyping(typingDto);
                }
            } catch (e) {
                console.error('[WS] Error parsing typing indicator', e);
            }
        });

        // 3. Subscribe to Read Receipts
        const readSub = stompClient.subscribe(`/topic/conversations/${conversationId}/read`, (msg) => {
            try {
                const receiptDto = JSON.parse(msg.body);
                if (handlers.onReadReceipt) {
                    handlers.onReadReceipt(receiptDto);
                }
            } catch (e) {
                console.error('[WS] Error parsing read receipt', e);
            }
        });

        activeSubscriptions.set('message', msgSub);
        activeSubscriptions.set('typing', typingSub);
        activeSubscriptions.set('read', readSub);
    }

    function unsubscribeConversation() {
        activeSubscriptions.forEach((sub) => {
            if (sub && sub.unsubscribe) {
                try {
                    sub.unsubscribe();
                } catch (e) {}
            }
        });
        activeSubscriptions.clear();
    }

    function sendMessage(conversationId, payload) {
        if (!stompClient || !isConnected) {
            return false;
        }

        try {
            stompClient.send(`/app/chat.send/${conversationId}`, {}, JSON.stringify(payload));
            return true;
        } catch (e) {
            console.warn('[WS] Send failed, falling back to REST', e);
            return false;
        }
    }

    function sendTyping(conversationId, isTyping) {
        if (!stompClient || !isConnected) return;
        try {
            stompClient.send(`/app/chat.typing/${conversationId}`, {}, JSON.stringify({ typing: isTyping }));
        } catch (e) {}
    }

    function sendReadReceipt(conversationId) {
        if (!stompClient || !isConnected) return;
        try {
            stompClient.send(`/app/chat.read/${conversationId}`, {}, JSON.stringify({}));
        } catch (e) {}
    }

    function disconnect() {
        if (stompClient && isConnected) {
            unsubscribeConversation();
            stompClient.disconnect();
            isConnected = false;
        }
    }

    function setCurrentUser(user) {
        if (user && user.id) {
            currentUserId = user.id;
            if (isConnected) {
                subscribeUserQueue(currentUserId);
            }
        }
    }

    return {
        connect,
        subscribeConversation,
        unsubscribeConversation,
        sendMessage,
        sendTyping,
        sendReadReceipt,
        disconnect,
        setCurrentUser,
        isConnected: () => isConnected
    };
})();
