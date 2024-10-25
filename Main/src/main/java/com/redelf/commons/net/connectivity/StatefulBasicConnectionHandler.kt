package com.redelf.commons.net.connectivity

import com.redelf.commons.registration.Registration

abstract class StatefulBasicConnectionHandler(

    defaultConnectionBlockState: ConnectionBlockingBehavior

) : BasicConnectivityHandler(defaultConnectionBlockState), Registration<ConnectivityStateChanges>