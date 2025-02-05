package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.Debuggable
import com.redelf.commons.connectivity.indicator.AvailableService
import com.redelf.commons.dependency.Chainable
import com.redelf.commons.registration.Registration
import com.redelf.commons.stateful.Stateful

interface AvailableStatefulService :

    Stateful,
    Debuggable,
    AvailableService,
    Registration<Stateful>,
    Chainable<AvailableStatefulService>