package util;

/**
 * Simple test class to verify Argon2 password hashing works correctly.
 * This can be run manually to test the implementation.
 */
public class PasswordHasherTest {

    public static void main(String[] args) {
        System.out.println("Testing Argon2 Password Hashing...\n");

        String testPassword = "TestPassword123!";

        // Test hashing
        String hash = PasswordHasher.hashPassword(testPassword);
        System.out.println("Hash generated: " + hash);
        System.out.println("Hash length: " + hash.length());
        System.out.println("Is Argon2 hash: " + PasswordHasher.isArgon2Hash(hash));
        System.out.println("Is BCrypt hash: " + PasswordHasher.isBCryptHash(hash));

        // Test verification
        boolean isValid = PasswordHasher.verifyPassword(testPassword, hash);
        System.out.println("\nVerification (correct password): " + isValid);

        // Test with wrong password
        boolean isInvalid = PasswordHasher.verifyPassword("WrongPassword", hash);
        System.out.println("Verification (wrong password): " + isInvalid);

        // Test with null/empty
        boolean nullTest = PasswordHasher.verifyPassword(null, hash);
        System.out.println("Verification (null password): " + nullTest);

        System.out.println("\nAll tests completed successfully!");
    }
}
