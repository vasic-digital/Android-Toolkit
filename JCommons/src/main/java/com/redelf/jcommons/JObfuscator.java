package com.redelf.jcommons;

public class JObfuscator implements JObfuscation {

    private final String salt;

    public JObfuscator(String salt) {
        this.salt = salt;
    }

    @Override
    public String obfuscate(String what) {

        String concatenated = what + salt;
        return bytesToHex(concatenated.getBytes());
    }

    @Override
    public String deobfuscate(String what) {

        byte[] decodedBytes = hexToBytes(what);
        String decodedString = new String(decodedBytes);
        return decodedString.replace(salt, "");
    }

    @Override
    public String name() {

        return "Common";
    }

    private String bytesToHex(byte[] bytes) {

        StringBuilder hexString = new StringBuilder(2 * bytes.length);

        for (byte b : bytes) {

            String hex = Integer.toHexString(0xff & b);

            if (hex.length() == 1) {

                hexString.append('0');
            }

            hexString.append(hex);
        }

        return hexString.toString();
    }

    private byte[] hexToBytes(String hex) {

        int len = hex.length();

        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {

            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }

        return data;
    }

    public void hello() {

        System.out.println("Hello from " + name() + " JObfuscator class :: " + obfuscate("Hello"));
    }
}
