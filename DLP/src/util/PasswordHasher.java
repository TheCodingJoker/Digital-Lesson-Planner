
package util;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class PasswordHasher {

    // OWASP-recommended Argon2 parameters
    private static final int ITERATIONS = 2;
    private static final int MEMORY = 65536; // 64 MB
    private static final int PARALLELISM = 1;
    private static final int SALT_LENGTH = 32;
    private static final int HASH_LENGTH = 64;

    private static final Argon2 argon2 = Argon2Factory.create();

    private PasswordHasher() {}

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        char[] passwordChars = plainPassword.toCharArray();
        try {
            return argon2.hash(ITERATIONS, MEMORY, PARALLELISM, passwordChars);
        } finally {
            // Clear password from memory
            java.util.Arrays.fill(passwordChars, '\0');
        }
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }

        char[] passwordChars = plainPassword.toCharArray();
        try {
            return argon2.verify(hashedPassword, passwordChars);
        } catch (Exception e) {
            return false;
        } finally {
            // Clear password from memory
            java.util.Arrays.fill(passwordChars, '\0');
        }
    }

}
