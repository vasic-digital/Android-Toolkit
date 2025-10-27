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
