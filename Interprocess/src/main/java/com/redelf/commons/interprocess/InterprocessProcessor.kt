package com.redelf.commons.interprocess

import android.content.Intent
import com.redelf.commons.processing.Process

interface InterprocessProcessor<RESULT> : Process<Intent, RESULT>