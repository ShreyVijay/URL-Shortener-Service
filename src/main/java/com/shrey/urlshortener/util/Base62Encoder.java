package com.shrey.urlshortener.util;

/**
 * Stateless Base62 encoder.
 *
 * <p>Converts a positive {@code long} (a database auto-increment id) into a
 * compact, URL-safe string using the 62-character alphabet:
 * {@code 0-9 a-z A-Z}.
 *
 * <p><b>Why Base62?</b> No special characters ({@code +}, {@code /}, {@code =}),
 * no external dependency, human-readable, and 62^6 ≈ 56.8 billion unique codes
 * at the minimum output length of 6 characters.
 *
 * <p><b>Collision guarantee:</b> the input is a {@code BIGSERIAL} primary key,
 * which is unique by definition. Each distinct id always produces a distinct code.
 *
 * <p>This class is a pure utility — no Spring context, no state.
 * It is trivially unit-testable without any mocking.
 */
public final class Base62Encoder {

    private static final String ALPHABET =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int BASE = ALPHABET.length();   // 62

    /** Minimum output length; left-padded with '0' when the encoded value is shorter. */
    private static final int MIN_LENGTH = 6;

    // Utility class — no instances.
    private Base62Encoder() {}

    /**
     * Encodes a positive {@code long} into a Base62 string.
     *
     * <p>The result is left-padded with {@code '0'} to at least {@value MIN_LENGTH}
     * characters so that all short codes have a consistent, predictable length.
     *
     * <pre>
     * Base62Encoder.encode(1)          // "000001"
     * Base62Encoder.encode(61)         // "0000z"  → padded to "00000z"
     * Base62Encoder.encode(3844)       // "001000"  (62^2 = 3844, i.e. "100" in base62 → padded)
     * Base62Encoder.encode(56_800_235_584L) // exactly 7 chars — no padding needed
     * </pre>
     *
     * @param number a positive database record id (must be &gt; 0)
     * @return the Base62-encoded short code, at least {@value MIN_LENGTH} characters long
     * @throws IllegalArgumentException if {@code number} is less than 1
     */
    public static String encode(long number) {
        if (number < 1) {
            throw new IllegalArgumentException(
                "Input must be a positive long (database id), got: " + number);
        }

        StringBuilder encoded = new StringBuilder();

        while (number > 0) {
            encoded.append(ALPHABET.charAt((int) (number % BASE)));
            number /= BASE;
        }

        // The digits were appended least-significant-first; reverse to get the correct order.
        encoded.reverse();

        // Left-pad with '0' to ensure a minimum output length of MIN_LENGTH characters.
        while (encoded.length() < MIN_LENGTH) {
            encoded.insert(0, '0');
        }

        return encoded.toString();
    }
}
