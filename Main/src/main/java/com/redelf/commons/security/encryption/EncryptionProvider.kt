package com.redelf.commons.security.encryption

import com.redelf.commons.obtain.suspendable.Obtain

interface EncryptionProvider : Obtain<Encryption<String, String>>