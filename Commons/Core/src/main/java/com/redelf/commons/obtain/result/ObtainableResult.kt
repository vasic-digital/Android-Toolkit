package com.redelf.commons.obtain.result

import com.redelf.commons.model.Wrapper

abstract class ObtainableResult<out T>(data: T) : Wrapper<T>(data)