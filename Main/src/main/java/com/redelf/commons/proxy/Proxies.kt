package com.redelf.commons.proxy

import com.redelf.commons.clear.Clearing
import com.redelf.commons.obtain.Obtain
import java.util.concurrent.PriorityBlockingQueue

interface Proxies<P : Proxy> : Obtain<PriorityBlockingQueue<P>>, Clearing