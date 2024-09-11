package com.redelf.jcommons;

public interface JObfuscation {

    String obfuscate(String what);

    String deobfuscate(String what);

    String name();
}
