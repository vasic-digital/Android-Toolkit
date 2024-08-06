package com.redelf.commons.interprocess

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

abstract class InterprocessWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params)