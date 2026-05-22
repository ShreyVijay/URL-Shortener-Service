package com.shrey.urlshortener.util;

public final class Base62Encoder {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Base62Encoder() {}

    public static String encode(long number) {
        if (number < 1) {
            throw new IllegalArgumentException("ID must be positive, got: " + number);
        }

        StringBuilder sb = new StringBuilder();

        while (number > 0) {
            sb.append(ALPHABET.charAt((int) (number % 62)));
            number /= 62;
        }

        return sb.reverse().toString();
    }
}
