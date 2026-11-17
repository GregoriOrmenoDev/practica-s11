package pe.edu.vallegrande.farmagro.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Component
final class PasswordHasher {

    private static final String BCRYPT_MARKER = "$2";
    private static final String BCRYPT_HASH_PATTERN = "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$";

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // Callers may send an existing hash; re-hashing would lock the user out.
    String hashIfPlain(String password) {
        return isHashed(password) ? password : encode(password);
    }

    boolean isHashed(String password) {
        return password != null && password.startsWith(BCRYPT_MARKER);
    }

    boolean matches(String rawPassword, String storedPassword) {
        return isHashed(storedPassword)
                ? matchesHash(rawPassword, storedPassword)
                : matchesLegacyPlainText(rawPassword, storedPassword);
    }

    private boolean matchesHash(String rawPassword, String storedHash) {
        if (!storedHash.matches(BCRYPT_HASH_PATTERN)) {
            log.warn("Stored password hash is malformed BCrypt data; login rejected");
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, storedHash);
        } catch (IllegalArgumentException hashException) {
            log.warn("Error verifying BCrypt hash: {}", hashException.getMessage());
            return false;
        }
    }

    // Legacy plain-text seeds; constant-time compare avoids timing leaks.
    private boolean matchesLegacyPlainText(String rawPassword, String storedPassword) {
        if (storedPassword == null || rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                storedPassword.getBytes(StandardCharsets.UTF_8),
                rawPassword.getBytes(StandardCharsets.UTF_8));
    }
}
