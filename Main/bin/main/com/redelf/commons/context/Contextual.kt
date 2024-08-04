package com.redelf.commons.context

import android.content.Context

interface Contextual<T : Context> : ContextAvailability<T>, ContextInjection<T>