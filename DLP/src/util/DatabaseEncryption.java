package util;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Protects the SQLite database file with Windows DPAPI for the current user.
 * Legacy AES files are supported for migration and are rewritten as DPAPI files
 * the next time the application shuts down.
 */
public class DatabaseEncryption {

    private static final byte[] DPAPI_FILE_HEADER =
        "DLP-DPAPI-1\n".getBytes(StandardCharsets.US_ASCII);
    private static final int DPAPI_UI_FORBIDDEN = 0x1;
    private static final String LEGACY_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int LEGACY_IV_SIZE = 16;

    private static final class WindowsApis {
        private static final Crypt32 CRYPT32 = Native.load(
            "Crypt32", Crypt32.class, W32APIOptions.DEFAULT_OPTIONS);
        private static final Kernel32 KERNEL32 = Native.load(
            "kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);
    }

    public interface Crypt32 extends StdCallLibrary {
        int CryptProtectData(
            DataBlob dataIn,
            WString description,
            DataBlob optionalEntropy,
            Pointer reserved,
            Pointer prompt,
            int flags,
            DataBlob dataOut);

        int CryptUnprotectData(
            DataBlob dataIn,
            PointerByReference description,
            DataBlob optionalEntropy,
            Pointer reserved,
            Pointer prompt,
            int flags,
            DataBlob dataOut);
    }

    public interface Kernel32 extends StdCallLibrary {
        Pointer LocalFree(Pointer memory);
    }

    @Structure.FieldOrder({"cbData", "pbData"})
    public static class DataBlob extends Structure implements Structure.ByReference {
        public int cbData;
        public Pointer pbData;
        private Memory memory;

        public DataBlob() {
        }

        public DataBlob(byte[] data) {
            cbData = data.length;
            if (data.length > 0) {
                memory = new Memory(data.length);
                memory.write(0, data, 0, data.length);
                pbData = memory;
            }
            write();
        }

        private byte[] toByteArray() throws IOException {
            read();
            if (cbData < 0 || (cbData > 0 && pbData == null)) {
                throw new IOException("Windows DPAPI returned an invalid data blob.");
            }
            return cbData == 0 ? new byte[0] : pbData.getByteArray(0, cbData);
        }
    }

    /**
     * Protects a file for the current Windows user and atomically replaces the
     * destination with a versioned DPAPI payload.
     */
    public static void encryptFile(String inputPath, String outputPath) throws Exception {
        requireWindows();
        byte[] plaintext = Files.readAllBytes(Paths.get(inputPath));
        byte[] protectedData = protectWithDpapi(plaintext);
        byte[] fileData = Arrays.copyOf(
            DPAPI_FILE_HEADER, DPAPI_FILE_HEADER.length + protectedData.length);
        System.arraycopy(protectedData, 0, fileData, DPAPI_FILE_HEADER.length, protectedData.length);
        writeAtomically(Paths.get(outputPath), fileData);
    }

    /**
     * Decrypts a DPAPI file. Files written by the previous AES implementation
     * are decrypted through the legacy path so shutdown can migrate them.
     */
    public static void decryptFile(String inputPath, String outputPath) throws Exception {
        requireWindows();
        byte[] fileData = Files.readAllBytes(Paths.get(inputPath));
        byte[] plaintext;

        if (hasDpapiHeader(fileData)) {
            byte[] protectedData = Arrays.copyOfRange(
                fileData, DPAPI_FILE_HEADER.length, fileData.length);
            plaintext = unprotectWithDpapi(protectedData);
        } else {
            plaintext = decryptLegacyAes(fileData);
        }

        Files.write(Paths.get(outputPath), plaintext);
    }

    public static boolean isEncrypted(String filePath) {
        Path temporaryFile = null;
        try {
            Path source = Paths.get(filePath).toAbsolutePath();
            temporaryFile = Files.createTempFile(source.getParent(), "dlp-decrypt-check-", ".tmp");
            decryptFile(filePath, temporaryFile.toString());
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException e) {
                    System.err.println("Could not remove database encryption check file: "
                        + e.getMessage());
                }
            }
        }
    }

    private static byte[] protectWithDpapi(byte[] plaintext) throws IOException {
        DataBlob input = new DataBlob(plaintext);
        DataBlob output = new DataBlob();
        int result = WindowsApis.CRYPT32.CryptProtectData(
            input,
            new WString("Digital Lesson Planner database"),
            null,
            null,
            null,
            DPAPI_UI_FORBIDDEN,
            output);
        if (result == 0) {
            free(output.pbData);
            throw new IOException("Windows DPAPI could not protect the database for this user.");
        }
        return copyAndFree(output, null);
    }

    private static byte[] unprotectWithDpapi(byte[] protectedData) throws IOException {
        DataBlob input = new DataBlob(protectedData);
        DataBlob output = new DataBlob();
        PointerByReference description = new PointerByReference();
        int result = WindowsApis.CRYPT32.CryptUnprotectData(
            input,
            description,
            null,
            null,
            null,
            DPAPI_UI_FORBIDDEN,
            output);
        if (result == 0) {
            free(output.pbData);
            free(description.getValue());
            throw new IOException(
                "Windows DPAPI could not decrypt the database for the current Windows user.");
        }
        return copyAndFree(output, description.getValue());
    }

    private static byte[] copyAndFree(DataBlob output, Pointer description) throws IOException {
        output.read();
        Pointer outputMemory = output.pbData;
        try {
            return output.toByteArray();
        } finally {
            free(outputMemory);
            free(description);
        }
    }

    private static void free(Pointer memory) {
        if (memory != null) {
            WindowsApis.KERNEL32.LocalFree(memory);
        }
    }

    private static boolean hasDpapiHeader(byte[] fileData) {
        if (fileData.length < DPAPI_FILE_HEADER.length) {
            return false;
        }
        for (int i = 0; i < DPAPI_FILE_HEADER.length; i++) {
            if (fileData[i] != DPAPI_FILE_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    private static void requireWindows() throws IOException {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            throw new IOException("Windows DPAPI database encryption requires Windows.");
        }
    }

    private static void writeAtomically(Path outputPath, byte[] fileData) throws IOException {
        Path absolutePath = outputPath.toAbsolutePath();
        Path parent = absolutePath.getParent();
        Path temporaryPath = Files.createTempFile(parent, "dlp-protected-db-", ".tmp");
        try {
            Files.write(temporaryPath, fileData);
            try {
                Files.move(temporaryPath, absolutePath,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporaryPath, absolutePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryPath);
        }
    }

    private static byte[] decryptLegacyAes(byte[] fileData) throws Exception {
        if (fileData.length <= LEGACY_IV_SIZE
                || (fileData.length - LEGACY_IV_SIZE) % LEGACY_IV_SIZE != 0) {
            throw new IOException("Database file is neither a recognized DPAPI nor legacy AES file.");
        }

        byte[] ivBytes = Arrays.copyOfRange(fileData, 0, LEGACY_IV_SIZE);
        byte[] encryptedBytes = Arrays.copyOfRange(fileData, LEGACY_IV_SIZE, fileData.length);
        Cipher cipher = Cipher.getInstance(LEGACY_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, deriveLegacyMachineKey(), new IvParameterSpec(ivBytes));
        return cipher.doFinal(encryptedBytes);
    }

    private static SecretKey deriveLegacyMachineKey() throws Exception {
        String machineIdentifier = System.getProperty("user.name")
            + System.getProperty("os.name")
            + System.getProperty("os.arch")
            + System.getProperty("os.version");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(machineIdentifier.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(hash, "AES");
    }
}
