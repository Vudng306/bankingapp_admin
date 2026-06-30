package org.nhom8.banking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.service.DeviceService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider       tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final DeviceService          deviceService;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            Long   userId   = tokenProvider.getUserIdFromToken(token);
            String deviceId = tokenProvider.getDeviceIdFromToken(token);

            UserDetails userDetails = userDetailsService.loadUserById(userId);

            if (userDetails.isEnabled() && userDetails.isAccountNonLocked()
                    && isDeviceAllowed(userId, deviceId)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Nếu token không có claim "did" → không cần kiểm tra (backward-compatible).
     * Nếu có "did" → thiết bị phải còn active; false = đã bị đăng xuất từ xa.
     */
    private boolean isDeviceAllowed(Long userId, String deviceId) {
        if (!StringUtils.hasText(deviceId)) return true;
        boolean active = deviceService.isDeviceActive(userId, deviceId);
        if (!active) log.debug("Remote logout enforced: userId={} deviceId={}", userId, deviceId);
        return active;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
