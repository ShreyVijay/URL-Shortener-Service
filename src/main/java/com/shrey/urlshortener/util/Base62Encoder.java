package com.shrey.urlshortener.util;

/**
 * Stateless Base62 encoder/decoder.
 *
 * <p>Converts a positive {@code long} (e.g. a database auto-increment id) into a
 * URL-safe, human-readable short code using a 62-character alphabet:
 * {@code 0-9 a-z A-Z}.
 *
 * <p>Collision guarantee: each distinct input produces a distinct output,
 * because the input (the DB primary key) is itself unique by definition.
 *
 * <p>Capacity: 62^6 ≈ 56.8 billion unique codes at minimum length 6.
 */
public final class Base62Encoder {

    private static final String ALPHABET =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();          // 62
    private static final int MIN_LENGTH = 6;

    // Utility class — no instances.
    private Base62Encoder() {}

    /**
     * Encodes a positive long into a Base62 string, left-padded with '0'
     * to at least {@value MIN_LENGTH} characters.
     *
     * @param id a positive database record id
     * @return the Base62-encoded short code
     * @throws IllegalArgumentException if {@code id} is less than 1
     */
    public static String encode(long id) {
        // TODO (M1-3-2): implement divide-and-mod encoding
        //  while id > 0: prepend alphabet[id % 62], id = id / 62
        //  then left-pad with '0' to MIN_LENGTH
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Decodes a Base62 string back to its original long value.
     * Intended for future use (e.g. admin tooling); not on the hot path.
     *
     * @param code a non-null, non-empty Base62 string
     * @return the original long value
     */
    public static long decode(String code) {
        // TODO: implement reverse lookup if needed
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
