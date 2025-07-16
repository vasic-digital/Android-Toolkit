package com.redelf.commons.lifecycle

import com.redelf.commons.lifecycle.initialization.InitializationCallback
import com.redelf.commons.lifecycle.shutdown.ShutdownCallback

interface LifecycleCallback<T> : InitializationCallback<T>, ShutdownCallback<T>