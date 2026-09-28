package util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Provides AES-256 encryption for the SQLite database file.
 * This implementation uses machine-specific key derivation to provide
 * encryption tied to the Windows machine, similar to DPAPI's purpose.
 */
public class DatabaseEncryption {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 16;

    /**
     * Derives an encryption key from machine-specific information.
     * This provides similar protection to DPAPI by tying encryption to the machine.
     */
    private static SecretKey deriveMachineKey() throws Exception {
        String machineIdentifier = System.getProperty("user.name") +
                                    System.getProperty("os.name") +
                                    System.getProperty("os.arch") +
                                    System.getProperty("os.version");

        // Use SHA-256 to create a 256-bit key from machine identifier
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(machineIdentifier.getBytes("UTF-8"));

        return new SecretKeySpec(hash, ALGORITHM);
    }

    /**
     * Generates a random IV for encryption.
     */
    private static IvParameterSpec generateIv() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    /**
     * Encrypts a file using AES-256-CBC.
     */
    public static void encryptFile(String inputPath, String outputPath) throws Exception {
        SecretKey key = deriveMachineKey();
        IvParameterSpec iv = generateIv();

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        // Read input file
        byte[] inputBytes = Files.readAllBytes(Paths.get(inputPath));

        // Encrypt
        byte[] encryptedBytes = cipher.doFinal(inputBytes);

        // Write IV + encrypted data to output
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(iv.getIV());
            fos.write(encryptedBytes);
        }
    }

    /**
     * Decrypts a file using AES-256-CBC.
     */
    public static void decryptFile(String inputPath, String outputPath) throws Exception {
        SecretKey key = deriveMachineKey();

        // Read IV and encrypted data
        byte[] fileBytes = Files.readAllBytes(Paths.get(inputPath));

        if (fileBytes.length < IV_SIZE) {
            throw new IOException("File too short to contain IV");
        }

        byte[] ivBytes = new byte[IV_SIZE];
        System.arraycopy(fileBytes, 0, ivBytes, 0, IV_SIZE);

        byte[] encryptedBytes = new byte[fileBytes.length - IV_SIZE];
        System.arraycopy(fileBytes, IV_SIZE, encryptedBytes, 0, encryptedBytes.length);

        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        // Write decrypted data
        Files.write(Paths.get(outputPath), decryptedBytes);
    }

    /**
     * Checks if a file is encrypted by checking if it can be decrypted.
     */
    public static boolean isEncrypted(String filePath) {
        try {
            // Create a temp file for testing
            String tempPath = filePath + ".temp";
            decryptFile(filePath, tempPath);
            Files.deleteIfExists(Paths.get(tempPath));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}