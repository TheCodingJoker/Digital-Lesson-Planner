package util;

/**
 * Simple test class to verify Argon2 password hashing works correctly.
 * This can be run manually to test the implementation.
 */
public class PasswordHasherTest {

    public static void main(String[] args) {
        String testPassword = "TestPassword123!";
        String hash = PasswordHasher.hashPassword(testPassword);
        assertTrue(PasswordHasher.verifyPassword(testPassword, hash),
            "The correct password should verify.");
        assertTrue(!PasswordHasher.verifyPassword("WrongPassword", hash),
            "A wrong password must not verify.");
        assertTrue(!PasswordHasher.verifyPassword(null, hash),
            "A null password must not verify.");
        assertTrue(!PasswordHasher.verifyPassword(testPassword, null),
            "A null hash must not verify.");

        boolean rejectedEmptyPassword = false;
        try {
            PasswordHasher.hashPassword("");
        } catch (IllegalArgumentException expected) {
            rejectedEmptyPassword = true;
        }
        assertTrue(rejectedEmptyPassword, "An empty password must not be hashed.");

        System.out.println("Password hashing checks passed.");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
