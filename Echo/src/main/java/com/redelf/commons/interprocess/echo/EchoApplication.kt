package com.redelf.commons.interprocess.echo

import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.redelf.commons.application.BaseApplication

class EchoApplication : BaseApplication() {

    override fun isProduction() = false

    override fun takeSalt(): String {

        return "echo_salt"
    }

    override fun onDoCreate() {
        super.onDoCreate()

        val echoWorkerRequest = OneTimeWorkRequest.Builder(EchoWorker::class.java).build()
        WorkManager.getInstance(applicationContext).enqueue(echoWorkerRequest)
    }
}