package com.redelf.commons.net.endpoint

import com.redelf.commons.destruction.clear.Clearing
import com.redelf.commons.obtain.suspendable.Obtain
import java.util.PriorityQueue

interface Endpoints<P : Endpoint> : Obtain<PriorityQueue<P>>, Clearing