
package util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    
    private static final int BCRYPT_ROUNDS = 12;

    private PasswordHasher() {}

    public static String hashPassword(String plainPassword) {
        String salt = BCrypt.gensalt(BCRYPT_ROUNDS);
        return BCrypt.hashpw(plainPassword, salt);
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
}
