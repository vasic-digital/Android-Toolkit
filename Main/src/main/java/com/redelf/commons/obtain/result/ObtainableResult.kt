package com.redelf.commons.obtain.result

import com.redelf.commons.data.model.Wrapper

abstract class ObtainableResult<T>(data: T) : Wrapper<T>(data)