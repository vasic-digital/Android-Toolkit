package com.redelf.commons.execution

interface CommonExecutionCallback {

    fun onExecution(success: Boolean, calledFrom: String)
}