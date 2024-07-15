package com.redelf.commons.proxy

import com.redelf.commons.clear.Clearing
import com.redelf.commons.obtain.Obtain
import java.util.PriorityQueue

interface Proxies<P : Proxy> : Obtain<PriorityQueue<P>>, Clearing