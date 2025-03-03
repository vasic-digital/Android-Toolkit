package com.redelf.commons.lifecycle

interface LifecycleCallback<T> : InitializationCallback<T>, ShutdownCallback<T>