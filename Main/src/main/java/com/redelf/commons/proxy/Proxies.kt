package com.redelf.commons.proxy

import com.redelf.commons.obtain.Obtain
import java.util.concurrent.PriorityBlockingQueue

interface Proxies : Obtain<PriorityBlockingQueue<Proxy>>