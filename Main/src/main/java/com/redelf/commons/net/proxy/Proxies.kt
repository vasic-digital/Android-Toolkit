package com.redelf.commons.net.proxy

import com.redelf.commons.destruction.clear.Clearing
import com.redelf.commons.obtain.suspendable.Obtain
import java.util.PriorityQueue

interface Proxies<P : Proxy> : Obtain<PriorityQueue<P>>, Clearing