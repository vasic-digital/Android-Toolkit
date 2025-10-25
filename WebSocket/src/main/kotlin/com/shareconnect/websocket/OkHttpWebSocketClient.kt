package com.shareconnect.websocket

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.WebSocket
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.math.pow

/**
 * OkHttp-based WebSocket client implementation with automatic reconnection,
 * subscription management, and connection state tracking.
 */
class OkHttpWebSocketClient(
    private val config: WebSocketConfig,
    private val messageParser: (String) -> WebSocketMessage?,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : WebSocketClient {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null

    private val subscriptions = mutableMapOf<String, (WebSocketMessage) -> Unit>()
    private val subscriptionLock = Any()

    // Statistics
    private val messagesSent = AtomicLong(0)
    private val messagesReceived = AtomicLong(0)
    private val bytesSent = AtomicLong(0)
    private val bytesReceived = AtomicLong(0)
    private var connectedAt: Long? = null
    private var reconnectAttempts = 0
    private var lastError: String? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutMillis, TimeUnit.MILLISECONDS)
        .readTimeout(config.readTimeoutMillis, TimeUnit.MILLISECONDS)
        .writeTimeout(config.writeTimeoutMillis, TimeUnit.MILLISECONDS)
        .pingInterval(config.pingIntervalMillis, TimeUnit.MILLISECONDS)
        .build()

    private var onConnectedCallback: () -> Unit = {}
    private var onDisconnectedCallback: (String) -> Unit = {}
    private var onErrorCallback: (Throwable) -> Unit = {}

    override suspend fun connect(
        onConnected: () -> Unit,
        onDisconnected: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) = withContext(Dispatchers.IO) {
        this@OkHttpWebSocketClient.onConnectedCallback = onConnected
        this@OkHttpWebSocketClient.onDisconnectedCallback = onDisconnected
        this@OkHttpWebSocketClient.onErrorCallback = onError

        if (_connectionState.value is ConnectionState.Connected) {
            return@withContext
        }

        _connectionState.value = ConnectionState.Connecting
        reconnectAttempts = 0

        performConnect()
    }

    private fun performConnect() {
        val requestBuilder = Request.Builder()
            .url(config.url)

        // Add custom headers
        config.headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val request = requestBuilder.build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                scope.launch {
                    handleConnected()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleMessage(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch {
                    handleClosing(code, reason)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch {
                    handleClosed(code, reason)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch {
                    handleFailure(t, response)
                }
            }
        })
    }

    private suspend fun handleConnected() {
        _connectionState.value = ConnectionState.Connected
        connectedAt = System.currentTimeMillis()
        reconnectAttempts = 0
        lastError = null

        startPingJob()

        withContext(Dispatchers.Main) {
            onConnectedCallback()
        }
    }

    private suspend fun handleMessage(text: String) {
        bytesReceived.addAndGet(text.length.toLong())
        messagesReceived.incrementAndGet()

        val message = try {
            messageParser(text)
        } catch (e: Exception) {
            lastError = "Message parse error: ${e.message}"
            null
        }

        if (message != null) {
            notifySubscribers(message)
        }
    }

    private fun handleClosing(code: Int, reason: String) {
        // Server initiated close, acknowledge it
        webSocket?.close(1000, "Client acknowledging close")
    }

    private suspend fun handleClosed(code: Int, reason: String) {
        stopPingJob()
        connectedAt = null

        _connectionState.value = ConnectionState.Disconnected

        withContext(Dispatchers.Main) {
            onDisconnectedCallback(reason)
        }

        // Attempt reconnection if enabled and not a normal closure
        if (config.reconnectEnabled && code != 1000 && reconnectAttempts < config.maxReconnectAttempts) {
            attemptReconnect()
        }
    }

    private suspend fun handleFailure(t: Throwable, response: Response?) {
        lastError = "${t.message} (Response: ${response?.code})"
        _connectionState.value = ConnectionState.Error(t)

        stopPingJob()
        connectedAt = null

        withContext(Dispatchers.Main) {
            onErrorCallback(t)
        }

        // Attempt reconnection if enabled
        if (config.reconnectEnabled && reconnectAttempts < config.maxReconnectAttempts) {
            attemptReconnect()
        }
    }

    private fun attemptReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts++
            _connectionState.value = ConnectionState.Reconnecting(reconnectAttempts, config.maxReconnectAttempts)

            // Exponential backoff
            val delay = min(
                config.reconnectDelayMillis * config.reconnectBackoffMultiplier.pow(reconnectAttempts - 1).toLong(),
                config.reconnectMaxDelayMillis
            )

            delay(delay)

            if (_connectionState.value !is ConnectionState.Connected) {
                performConnect()
            }
        }
    }

    override suspend fun disconnect(code: Int, reason: String) = withContext(Dispatchers.IO) {
        reconnectJob?.cancel()
        stopPingJob()

        webSocket?.close(code, reason)
        webSocket = null

        _connectionState.value = ConnectionState.Disconnected
        connectedAt = null
    }

    override suspend fun send(message: WebSocketMessage): Boolean = withContext(Dispatchers.IO) {
        val ws = webSocket
        if (ws == null || _connectionState.value !is ConnectionState.Connected) {
            return@withContext false
        }

        try {
            val json = message.toJson()
            val success = ws.send(json)

            if (success) {
                messagesSent.incrementAndGet()
                bytesSent.addAndGet(json.length.toLong())
            }

            success
        } catch (e: Exception) {
            lastError = "Send error: ${e.message}"
            false
        }
    }

    override fun subscribe(messageType: String, callback: (WebSocketMessage) -> Unit): String {
        val subscriptionId = UUID.randomUUID().toString()

        synchronized(subscriptionLock) {
            subscriptions["$messageType:$subscriptionId"] = callback
        }

        return subscriptionId
    }

    override fun unsubscribe(subscriptionId: String) {
        synchronized(subscriptionLock) {
            subscriptions.keys.removeAll { it.endsWith(":$subscriptionId") }
        }
    }

    override fun clearSubscriptions() {
        synchronized(subscriptionLock) {
            subscriptions.clear()
        }
    }

    override fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected
    }

    override fun getStats(): ConnectionStats {
        return ConnectionStats(
            connectedAt = connectedAt,
            messagesSent = messagesSent.get(),
            messagesReceived = messagesReceived.get(),
            bytesSent = bytesSent.get(),
            bytesReceived = bytesReceived.get(),
            reconnectAttempts = reconnectAttempts,
            lastError = lastError
        )
    }

    private fun notifySubscribers(message: WebSocketMessage) {
        val matchingSubscriptions = synchronized(subscriptionLock) {
            subscriptions.filter { it.key.startsWith("${message.type}:") }
        }

        matchingSubscriptions.values.forEach { callback ->
            scope.launch {
                try {
                    callback(message)
                } catch (e: Exception) {
                    lastError = "Subscription callback error: ${e.message}"
                }
            }
        }
    }

    private fun startPingJob() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && _connectionState.value is ConnectionState.Connected) {
                delay(config.pingIntervalMillis)
                // OkHttp handles ping/pong automatically, this is just for keepalive
            }
        }
    }

    private fun stopPingJob() {
        pingJob?.cancel()
        pingJob = null
    }

    /**
     * Cleanup resources
     */
    fun close() {
        scope.launch {
            disconnect()
        }
        scope.cancel()
        okHttpClient.dispatcher.executorService.shutdown()
        okHttpClient.connectionPool.evictAll()
    }
}
