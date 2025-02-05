package com.redelf.commons.net.proxy

import com.redelf.commons.net.remote.Remote
import com.redelf.commons.timeout.Timeout

abstract class Proxy(

    var schema: String = "http",
    var address: String,
    var port: Int

) : Remote, Comparable<Proxy>, Timeout