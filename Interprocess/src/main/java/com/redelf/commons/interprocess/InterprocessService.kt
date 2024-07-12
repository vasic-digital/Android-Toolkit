package com.redelf.commons.interprocess

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

abstract class InterprocessService(ctx: Context, params: WorkerParameters) : Worker(ctx, params)