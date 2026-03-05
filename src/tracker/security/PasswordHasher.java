package tracker.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password hashing utility using SHA-256 with per-password salt.
 *
 * Storage format: "salt:hash" where both are Base64-encoded.
 *
 * BCrypt would be ideal but requires an external dependency.
 * SHA-256 + salt is acceptable for a local desktop application
 * with no network exposure. Never stores plain text.
 */
public final class PasswordHasher {

    private static final int SALT_LENGTH = 16;

    /**
     * Hashes a plain-text password with a random salt.
     *
     * @param plainPassword the password to hash
     * @return the stored form "salt:hash" (Base64-encoded)
     */
    public static String hash(String plainPassword) {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] hashed = hashWithSalt(plainPassword, salt);
        return Base64.getEncoder().encodeToString(salt) + ":"
             + Base64.getEncoder().encodeToString(hashed);
    }

    /**
     * Verifies a plain-text password against a stored "salt:hash" string.
     *
     * @param plainPassword the password to check
     * @param storedHash    the stored "salt:hash" value from the database
     * @return true if the password matches
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        String[] parts = storedHash.split(":");
        if (parts.length != 2) return false;
        try {
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            byte[] actualHash = hashWithSalt(plainPassword, salt);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] hashWithSalt(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private PasswordHasher() { }
}
