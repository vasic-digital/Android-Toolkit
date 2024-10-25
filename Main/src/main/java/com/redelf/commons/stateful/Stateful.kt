package com.redelf.commons.stateful

interface Stateful : GetState<Int>, SetState<Int>, OnState<Int>