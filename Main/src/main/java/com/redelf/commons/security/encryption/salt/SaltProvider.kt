package com.redelf.commons.security.encryption.salt

import com.redelf.commons.obtain.suspendable.Obtain

interface SaltProvider<SALT> : Obtain<SALT>