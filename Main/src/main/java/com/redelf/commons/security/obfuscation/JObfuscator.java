package com.redelf.commons.security.obfuscation;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class JObfuscator {

    private final String salt;

    public JObfuscator(String salt) {

        this.salt = salt;
    }

    String obfuscate(String what) {

        return Base64.getEncoder().encodeToString((what + salt).getBytes(StandardCharsets.UTF_8));
    }

    String deobfuscate(String what) {

        return new String(Base64.getDecoder().decode(what), StandardCharsets.UTF_8).replace(salt, "");
    }
}
