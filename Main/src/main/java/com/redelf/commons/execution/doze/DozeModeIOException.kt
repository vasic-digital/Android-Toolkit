package com.redelf.commons.execution.doze

import java.io.IOException

class DozeModeIOException(message: String, cause: Throwable?) : IOException(message, cause)