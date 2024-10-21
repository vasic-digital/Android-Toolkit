package com.redelf.commons.connectivity.indicator

import com.redelf.commons.registration.Registration
import com.redelf.commons.stateful.Stateful

interface AvailableStatefulService<T> : AvailableService, Stateful<T>, Registration<Stateful<T>>