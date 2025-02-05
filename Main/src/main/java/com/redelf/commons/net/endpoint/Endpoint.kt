package com.redelf.commons.net.endpoint

import com.redelf.commons.net.remote.Remote
import com.redelf.commons.timeout.Timeout

abstract class Endpoint(var address: String) : Remote, Comparable<Endpoint>, Timeout