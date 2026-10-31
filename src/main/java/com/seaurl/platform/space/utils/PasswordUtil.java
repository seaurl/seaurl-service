package com.seaurl.platform.space.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final String SALT = "seaurl_password_manager_2024";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public static String encrypt(String plaintext, String uid) {
        try {
            byte[] key = deriveKey(uid);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] output = cipher.doFinal(plaintext.getBytes("UTF-8"));

            int tagLen = GCM_TAG_LENGTH / 8;
            byte[] ciphertext = new byte[output.length - tagLen];
            byte[] authTag = new byte[tagLen];
            System.arraycopy(output, 0, ciphertext, 0, ciphertext.length);
            System.arraycopy(output, ciphertext.length, authTag, 0, tagLen);

            return Base64.getEncoder().encodeToString(ciphertext) + ":"
                    + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(authTag);
        } catch (Exception e) {
            throw new RuntimeException("Password encryption failed", e);
        }
    }

    public static String decrypt(String encrypted, String uid) {
        try {
            String[] parts = encrypted.split(":");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid encrypted password format");
            }

            byte[] ciphertext = Base64.getDecoder().decode(parts[0]);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] authTag = Base64.getDecoder().decode(parts[2]);
            byte[] key = deriveKey(uid);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            cipher.update(ciphertext);
            byte[] plaintext = cipher.doFinal(authTag);

            return new String(plaintext, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Password decryption failed", e);
        }
    }

    private static byte[] deriveKey(String uid) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String input = uid + SALT;
        return digest.digest(input.getBytes("UTF-8"));
    }
}
