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


package com.shareconnect.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Unified search interface for ShareConnect ecosystem
 * Enables searching across all connected services from a single interface
 */
interface UnifiedSearch {

    /**
     * Search across all connected services
     * @param query Search query
     * @param services List of services to search (empty = all services)
     * @return Flow of search results
     */
    fun searchAll(query: String, services: List<String> = emptyList()): Flow<SearchResult>

    /**
     * Search within a specific service
     * @param service Service identifier
     * @param query Search query
     * @return Flow of search results for that service
     */
    fun searchService(service: String, query: String): Flow<SearchResult>
}

/**
 * Search result data class
 */
data class SearchResult(
    val service: String,
    val title: String,
    val description: String?,
    val url: String?,
    val thumbnailUrl: String?,
    val type: SearchResultType,
    val relevanceScore: Float = 1.0f
)

/**
 * Type of search result
 */
enum class SearchResultType {
    VIDEO, AUDIO, DOCUMENT, IMAGE, TORRENT, CONTAINER, SERVER, OTHER
}

/**
 * Default implementation of unified search
 */
class ShareConnectUnifiedSearch : UnifiedSearch {

    private val searchProviders = mutableMapOf<String, SearchProvider>()

    fun registerProvider(service: String, provider: SearchProvider) {
        searchProviders[service] = provider
    }

    override fun searchAll(query: String, services: List<String>): Flow<SearchResult> = flow {
        val targetServices = if (services.isEmpty()) searchProviders.keys else services

        targetServices.forEach { service ->
            searchProviders[service]?.let { provider ->
                try {
                    provider.search(query).collect { result ->
                        emit(result)
                    }
                } catch (e: Exception) {
                    // Log error but continue with other services
                    e.printStackTrace()
                }
            }
        }
    }

    override fun searchService(service: String, query: String): Flow<SearchResult> = flow {
        searchProviders[service]?.let { provider ->
            provider.search(query).collect { result ->
                emit(result)
            }
        }
    }
}

/**
 * Interface for service-specific search providers
 */
interface SearchProvider {
    fun search(query: String): Flow<SearchResult>
}
