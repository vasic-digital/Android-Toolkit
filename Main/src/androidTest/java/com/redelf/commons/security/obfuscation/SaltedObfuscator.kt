package com.redelf.commons.security.obfuscation

abstract class SaltedObfuscator(protected val salt: String) : Obfuscation