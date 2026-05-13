package com.orderly.orderly_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * The single security gateway for all HTTP requests.
     *
     * - CSRF disabled: the API is stateless (JWT, no session cookies), so CSRF
     *   attacks don't apply.
     * - Sessions disabled: we never create an HTTP session; the JWT is the only
     *   authentication state.
     * - Some Auth endpoints are public; everything else requires a valid JWT.
     * - JWT validation is delegated to jwtDecoder() below.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public auth endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/password-reset").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/password-reset/confirm").permitAll()
                // OpenAPI / Swagger UI — exposes API shape only, no sensitive data.
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Everything else requires a valid JWT in the Authorization header.
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                // Validates the JWT on every protected request and populates
                // SecurityContextHolder so service methods can read the principal
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
                // Return the ErrorResponse envelope on 401 instead of Spring's default
                .authenticationEntryPoint(unauthorizedEntryPoint())
            );
        return http.build();
    }

    /**
     * Validates incoming JWTs using HS256
     *
     * This bean only VALIDATES tokens. The auth module issues tokens at login
     * using the same secret, so both sides agree on how to sign and verify.
     *
     * Spring Security extracts the JWT from the Authorization header, verifies
     * its signature here, checks the expiry claim, and — if valid — injects a
     * JwtAuthenticationToken into SecurityContextHolder for the service layer
     * to read via SecurityContextHolder.getContext().getAuthentication().
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    /**
     * Allows the React dev server (localhost:5173) to call this API.
     *
     * Browsers enforce the Same-Origin Policy: a page on one origin cannot
     * call a different origin unless the server explicitly allows it via CORS
     * headers. During development, React runs on port 5173 and the API on 8080
     * — different ports means different origins.
     *
     * In production, nginx serves the React build and proxies /api/* to this
     * process, all on the same domain. The browser never sees a cross-origin
     * request there, so these CORS headers are simply never triggered.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Required so the browser sends the Authorization header cross-origin
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Returns the ErrorResponse JSON envelope on 401s triggered by Spring Security
     * (missing, malformed, or expired JWT) so the response matches the OpenAPI spec
     * instead of Spring's default error format.
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
                -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                new ObjectMapper().writeValueAsString(Map.of(
                    "status", 401,
                    "error", "UNAUTHORIZED",
                    "message", "Missing or invalid Bearer token."
                ))
            );
        };
    }

    /**
     * BCrypt password encoder — declared here (not in auth/) so it is available
     * to any module without creating cross-module bean dependencies. The auth
     * module injects this to hash passwords on registration and verify them on
     * login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
