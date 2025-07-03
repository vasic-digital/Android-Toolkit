package com.redelf.commons.security.obfuscation

abstract class SaltedObfuscator(val saltProvider: ObfuscatorSaltProvider) : ObfuscationAsync