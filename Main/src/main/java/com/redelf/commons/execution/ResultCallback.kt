package com.redelf.commons.execution

/**
 * Callback interface for async execution results
 */
interface ResultCallback<T> {
    
    /**
     * Called when the operation completes successfully
     */
    fun onSuccess(result: T)
    
    /**
     * Called when the operation fails
     */
    fun onFailure(error: Throwable)
}