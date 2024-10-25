package com.redelf.commons.stateful

interface Stateful<T> : GetState<T>, SetState<T>, OnState<T>