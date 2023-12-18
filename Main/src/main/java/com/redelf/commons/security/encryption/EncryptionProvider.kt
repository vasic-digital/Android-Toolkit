package com.redelf.commons.security.encryption

import com.redelf.commons.obtain.Obtain

interface EncryptionProvider : Obtain<Encryption<String, String>>