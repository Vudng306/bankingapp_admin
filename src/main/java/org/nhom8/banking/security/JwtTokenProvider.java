package org.nhom8.banking.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /** Claim key lưu deviceId trong JWT — "did" = device id */
    private static final String DEVICE_ID_CLAIM = "did";

    // ── Generate ────────────────────────────────────────────────────────────

    /**
     * Sinh JWT không gắn deviceId — dùng cho register/verify-otp và
     * mọi luồng không có thông tin thiết bị.
     */
    public String generateToken(Long userId) {
        return generateToken(userId, null);
    }

    /**
     * Sinh JWT, gắn thêm claim "did" = deviceId nếu khác null/blank.
     * Token có "did" sẽ bị filter kiểm tra is_active trên mỗi request.
     */
    public String generateToken(Long userId, String deviceId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry);

        if (deviceId != null && !deviceId.isBlank()) {
            builder.claim(DEVICE_ID_CLAIM, deviceId);
        }

        return builder.signWith(signingKey()).compact();
    }

    // ── Extract ─────────────────────────────────────────────────────────────

    public Long getUserIdFromToken(String token) {
        String subject = parseClaims(token).getSubject();
        return Long.parseLong(subject);
    }

    /** Trả về deviceId từ claim "did", hoặc null nếu token không có claim này. */
    public String getDeviceIdFromToken(String token) {
        return parseClaims(token).get(DEVICE_ID_CLAIM, String.class);
    }

    public Date getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration();
    }

    // ── Validate ─────────────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("JWT unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("JWT malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.debug("JWT signature invalid: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT empty/null: {}", e.getMessage());
        }
        return false;
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
