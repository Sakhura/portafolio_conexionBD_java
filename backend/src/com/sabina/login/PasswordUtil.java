package com.sabina.login;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilidad de hashing de contrasenas.
 * Usa SHA-256 + salt aleatorio por usuario (sin dependencias externas).
 * Para un sistema en produccion se recomienda BCrypt o Argon2.
 */
public class PasswordUtil {

    public static String generarSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().withoutPadding().encodeToString(bytes).substring(0, 16);
    }

    public static String hashear(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
