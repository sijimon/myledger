package com.myledger.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myledger.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
import java.util.Map;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({SecurityProperties.class, CorsProperties.class})
public class SecurityConfig {

    private static final String OWNER = "ROLE_OWNER";

    private final CorsProperties corsProperties;

    public SecurityConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           ObjectMapper objectMapper) throws Exception {
        // Only enable CORS when origins are explicitly configured (e.g. the Vite dev
        // server on a different origin). When empty, the SPA is served same-origin via
        // nginx, so an empty allow-list would wrongly 403 the browser's same-origin
        // POSTs (which still carry an Origin header). In that case disable CORS entirely.
        List<String> origins = corsProperties.allowedOrigins();
        if (origins == null || origins.isEmpty()) {
            http.cors(AbstractHttpConfigurer::disable);
        } else {
            http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        }

        http
                // Stateless API secured by Bearer tokens; the refresh cookie is SameSite-guarded.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Let error forwards render the real status/body instead of being
                        // re-intercepted and masked as a generic 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Owner-only surfaces.
                        .requestMatchers("/api/contractors/**").hasAuthority(OWNER)
                        .requestMatchers("/api/users/**").hasAuthority(OWNER)
                        .requestMatchers("/api/roles/**").hasAuthority(OWNER)
                        // Expenses (all methods) + dashboard summary + finance years are
                        // authenticated; the controllers/services enforce tab access, ownership,
                        // and project-assignment scoping for contractors.
                        .requestMatchers("/api/categories/**").hasAuthority(OWNER)
                        .requestMatchers("/api/projects/**").hasAuthority(OWNER)
                        .requestMatchers("/api/phases/**").hasAuthority(OWNER)
                        .requestMatchers("/api/files/**").hasAuthority(OWNER)
                        .requestMatchers(HttpMethod.POST, "/api/invoices").hasAuthority(OWNER)
                        .requestMatchers(HttpMethod.GET, "/api/invoices").hasAuthority(OWNER)
                        // Everything else requires a valid token; per-resource ownership is
                        // enforced in the service layer.
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, deniedException) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, "Forbidden")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletResponse response, ObjectMapper mapper, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(), Map.of("status", status, "error", message));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = corsProperties.allowedOrigins();
        config.setAllowedOrigins(origins == null ? List.of() : origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true); // refresh cookie
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
