package com.redelf.commons.interprocess.echo

import android.content.Context
import android.content.Intent
import androidx.work.WorkerParameters
import com.redelf.commons.interprocess.InterprocessWorker

class EchoWorker(ctx: Context, params: WorkerParameters) : InterprocessWorker(ctx, params) {

    override fun onIntent(intent: Intent) {

        TODO("Not yet implemented")
    }

    override fun doWork(): Result {

        TODO("Not yet implemented")
    }
}