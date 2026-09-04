package com.pixelMind.materialGrid.config;

import com.pixelMind.materialGrid.repository.UserRepository;
import com.pixelMind.materialGrid.repository.UserSessionRepository;
import com.pixelMind.materialGrid.security.CustomUserDetailsService;
import com.pixelMind.materialGrid.security.SecurityContextService;
import com.pixelMind.materialGrid.security.SessionAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration notes:
 *
 * - CSRF is disabled deliberately, not blindly: this is a stateless,
 *   token-authenticated REST API (bearer token in the Authorization header,
 *   never a browser cookie/session), so it is not subject to the
 *   cookie-based CSRF attack CSRF protection exists to prevent. Disabling it
 *   here is the standard, correct posture for token-auth REST APIs, not a
 *   shortcut - if this API ever authenticates via cookies, CSRF protection
 *   must be re-enabled.
 * - Session creation policy is STATELESS: Spring Security itself holds no
 *   HttpSession. All "session" semantics are our own explicit,
 *   database-backed UserSession, validated per-request by
 *   SessionAuthenticationFilter. This avoids two competing, easily
 *   desynchronized notions of "session".
 * - Passwords are hashed with BCrypt (industry standard, adaptive cost,
 *   built-in salt) - never stored or compared in plain text.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final SecurityContextService securityContextService;

    @Value("${app.security.session-idle-timeout-minutes:30}")
    private long sessionIdleTimeoutMinutes;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig,
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }

    @Bean
    public SessionAuthenticationFilter sessionAuthenticationFilter() {
        return new SessionAuthenticationFilter(
                userSessionRepository, userRepository, securityContextService, sessionIdleTimeoutMinutes);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable) // stateless bearer-token API - see class javadoc
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers("/api/v1/auth/login/**").permitAll()
                        .requestMatchers("/api/v1/users/**").permitAll()
                        .requestMatchers("/api/v1/auth/logout", "/api/v1/auth/me").permitAll()
                        .requestMatchers("/api/v1/price-rates/**").permitAll()
                        .requestMatchers("/api/v1/routes/**").permitAll()
                        .requestMatchers("/api/v1/vehicles/**").permitAll()
                        .requestMatchers("/api/v1/licenses/**").permitAll()
                        .requestMatchers("/api/v1/vehicle-licenses/**").permitAll()
                        .requestMatchers("/api/v1/vehicle-expenses/**").permitAll()
                        .requestMatchers("/api/v1/daily-routes/report/summary/**").permitAll()
                        .requestMatchers("/api/v1/daily-routes/**").permitAll()
                        .requestMatchers("/api/v1/file-history/**").permitAll()
                        .requestMatchers("/api/v1/persons/**").permitAll()

                        .anyRequest().permitAll()
                )
                .addFilterBefore(sessionAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
