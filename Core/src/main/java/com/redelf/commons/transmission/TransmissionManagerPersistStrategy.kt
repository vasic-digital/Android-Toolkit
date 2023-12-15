package com.redelf.commons.transmission

interface TransmissionManagerPersistStrategy {

    fun persist(identifier: String, data: String): Boolean
}