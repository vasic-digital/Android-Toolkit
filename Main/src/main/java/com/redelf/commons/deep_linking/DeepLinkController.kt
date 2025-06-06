package com.redelf.commons.deep_linking

interface DeepLinkController {

    fun handleDeepLink(parameter: String, onSuccess: () -> Unit, onError: () -> Unit)
}