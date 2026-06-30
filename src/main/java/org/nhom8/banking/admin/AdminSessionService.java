package org.nhom8.banking.admin;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AdminSessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.session-minutes:480}")
    private long sessionMinutes;

    private final Map<String, Instant> sessions = new ConcurrentHashMap<>();

    public Map<String, Object> login(String username, String password) {
        cleanupExpired();
        if (!constantTimeEquals(adminUsername, username) || !constantTimeEquals(adminPassword, password)) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Tài khoản admin không đúng");
        }

        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Instant expiresAt = Instant.now().plusSeconds(sessionMinutes * 60);
        sessions.put(token, expiresAt);

        return Map.of(
                "token", token,
                "expiresAt", expiresAt.toString(),
                "expiresIn", sessionMinutes * 60,
                "username", adminUsername
        );
    }

    public void requireValid(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Thiếu X-Admin-Token");
        }
        Instant expiresAt = sessions.get(token);
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            sessions.remove(token);
            throw new AppException(ErrorCode.UNAUTHORIZED, "Phiên admin đã hết hạn hoặc không hợp lệ");
        }
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, Instant>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isBefore(now)) it.remove();
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) return false;
        byte[] a = expected.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            byte av = i < a.length ? a[i] : 0;
            byte bv = i < b.length ? b[i] : 0;
            diff |= av ^ bv;
        }
        return diff == 0;
    }
}
