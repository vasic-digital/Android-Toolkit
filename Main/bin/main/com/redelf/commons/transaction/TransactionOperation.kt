package com.redelf.commons.transaction

import com.redelf.commons.session.SessionOperation

interface TransactionOperation : SessionOperation {

    override fun start() = true

    override fun end() = true
}