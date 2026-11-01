/**
 * Connect Chat - STOMP & SockJS Real-Time WebSocket Client
 */

const ChatWebSocket = (() => {
    let stompClient = null;
    let isConnected = false;
    let activeSubscriptions = new Map();
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

    function connect(callbacks = {}) {
        Object.assign(handlers, callbacks);

        // Fallback: Use SockJS if available, otherwise native WebSocket
        const socket = typeof SockJS !== 'undefined' ? new SockJS('/ws') : new WebSocket(`ws://${window.location.host}/ws`);
        stompClient = Stomp.over(socket);

        // Disable verbose console logging in production
        stompClient.debug = () => {};

        stompClient.connect({}, onConnected, onError);
    }

    function onConnected(frame) {
        isConnected = true;
        reconnectAttempts = 0;
        console.log('[WS] Connected to STOMP broker');

        // Subscribe to global presence channel
        subscribePresence();

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
            setTimeout(() => connect(handlers), timeout);
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

    function subscribeConversation(conversationId) {
        if (!stompClient || !isConnected || !conversationId) return;

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
                console.error('[WS] Error parsing message payload', e);
            }
        });

        // 2. Subscribe to Typing indicators
        const typingSub = stompClient.subscribe(`/topic/conversations/${conversationId}/typing`, (msg) => {
            try {
                const typingDto = JSON.parse(msg.body);
                if (handlers.onTyping) {
                    handlers.onTyping(typingDto);
                }
            } catch (e) {
                console.error('[WS] Error parsing typing payload', e);
            }
        });

        // 3. Subscribe to Read receipts
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
            console.warn('[WS] STOMP not connected, falling back to REST');
            return false;
        }

        stompClient.send(`/app/chat.send/${conversationId}`, {}, JSON.stringify(payload));
        return true;
    }

    function sendTyping(conversationId, isTyping) {
        if (!stompClient || !isConnected) return;
        stompClient.send(`/app/chat.typing/${conversationId}`, {}, JSON.stringify({ typing: isTyping }));
    }

    function sendReadReceipt(conversationId) {
        if (!stompClient || !isConnected) return;
        stompClient.send(`/app/chat.read/${conversationId}`, {}, JSON.stringify({}));
    }

    function disconnect() {
        if (stompClient && isConnected) {
            unsubscribeConversation();
            stompClient.disconnect();
            isConnected = false;
        }
    }

    return {
        connect,
        disconnect,
        subscribeConversation,
        unsubscribeConversation,
        sendMessage,
        sendTyping,
        sendReadReceipt,
        isConnected: () => isConnected
    };
})();

