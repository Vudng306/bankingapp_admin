package org.nhom8.banking.config;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.security.JwtAuthenticationFilter;
import org.nhom8.banking.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl  userDetailsService;
    private final ObjectMapper            objectMapper;

    private static final String[] PUBLIC_URLS = {
            "/auth/register",
            "/auth/verify-otp",
            "/auth/login",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/resend-otp",
            "/admin",
            "/admin/",
            "/admin/**",
            "/admin-api/**",
            "/favicon.ico",
            "/error"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_URLS).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                    writeError(res, HttpStatus.UNAUTHORIZED, "Chưa xác thực, vui lòng đăng nhập"))
                .accessDeniedHandler((req, res, e) ->
                    writeError(res, HttpStatus.FORBIDDEN, "Không có quyền truy cập"))
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Tắt auto-registration của JwtAuthenticationFilter như một servlet filter thông thường.
     * Filter chỉ chạy trong Spring Security filter chain (addFilterBefore).
     * Nếu không tắt: filter chạy 2 lần — lần đầu ngoài Spring Security chain thì set auth,
     * nhưng SecurityContextHolderFilter sẽ clear context trước khi vào security chain,
     * và OncePerRequestFilter bỏ qua lần 2 → kết quả: 401 mặc dù token hợp lệ.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void writeError(jakarta.servlet.http.HttpServletResponse res,
                            HttpStatus status, String message) throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(message)));
    }
}
