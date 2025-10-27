/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.shareconnect.websocket

import kotlinx.coroutines.flow.StateFlow

/**
 * WebSocket client interface for real-time bidirectional communication.
 * Implementations provide automatic reconnection, message handling, and connection state management.
 */
interface WebSocketClient {
    /**
     * Current connection state flow
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Connect to the WebSocket server
     * @param onConnected Callback invoked when connection is established
     * @param onDisconnected Callback invoked when connection is lost
     * @param onError Callback invoked when an error occurs
     */
    suspend fun connect(
        onConnected: () -> Unit = {},
        onDisconnected: (reason: String) -> Unit = {},
        onError: (error: Throwable) -> Unit = {}
    )

    /**
     * Disconnect from the WebSocket server
     * @param code Close code (1000 = normal closure)
     * @param reason Human-readable reason for closing
     */
    suspend fun disconnect(code: Int = 1000, reason: String = "Normal closure")

    /**
     * Send a message to the server
     * @param message Message to send
     * @return true if message was sent successfully, false otherwise
     */
    suspend fun send(message: WebSocketMessage): Boolean

    /**
     * Subscribe to messages matching a specific type/channel
     * @param messageType Type of message to subscribe to
     * @param callback Callback invoked when matching message is received
     * @return Subscription ID for later unsubscription
     */
    fun subscribe(messageType: String, callback: (WebSocketMessage) -> Unit): String

    /**
     * Unsubscribe from messages
     * @param subscriptionId Subscription ID returned from subscribe()
     */
    fun unsubscribe(subscriptionId: String)

    /**
     * Clear all subscriptions
     */
    fun clearSubscriptions()

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean

    /**
     * Get connection statistics
     */
    fun getStats(): ConnectionStats
}

/**
 * WebSocket connection state
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val error: Throwable) : ConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionState()
}

/**
 * Base class for WebSocket messages
 */
abstract class WebSocketMessage {
    abstract val type: String
    abstract fun toJson(): String
}

/**
 * Connection statistics
 */
data class ConnectionStats(
    val connectedAt: Long?,
    val messagesSent: Long,
    val messagesReceived: Long,
    val bytesSent: Long,
    val bytesReceived: Long,
    val reconnectAttempts: Int,
    val lastError: String?
)

/**
 * WebSocket configuration
 */
data class WebSocketConfig(
    val url: String,
    val reconnectEnabled: Boolean = true,
    val maxReconnectAttempts: Int = 5,
    val reconnectDelayMillis: Long = 1000,
    val reconnectMaxDelayMillis: Long = 30000,
    val reconnectBackoffMultiplier: Double = 2.0,
    val pingIntervalMillis: Long = 30000,
    val pongTimeoutMillis: Long = 10000,
    val connectTimeoutMillis: Long = 10000,
    val readTimeoutMillis: Long = 0, // 0 = no timeout
    val writeTimeoutMillis: Long = 10000,
    val headers: Map<String, String> = emptyMap()
)
