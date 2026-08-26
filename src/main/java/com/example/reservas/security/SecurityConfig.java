package com.example.reservas.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final Environment environment;

    public SecurityConfig(JwtFilter jwtFilter, Environment environment) {
        this.jwtFilter = jwtFilter;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeHttpRequests(auth -> {
                auth
                    // --- Allowlist publica (solo lo imprescindible) ---
                    // Alta de usuario sin sesion previa
                    .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                    // Autenticacion inicial (access + refresh cookie)
                    .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                    // Verificacion de email post-registro
                    .requestMatchers(HttpMethod.POST, "/auth/confirm").permitAll()
                    // Reenvio del codigo de confirmacion
                    .requestMatchers(HttpMethod.POST, "/auth/resend").permitAll()
                    // Renovacion de access token via cookie httpOnly
                    .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                    // Invalidacion de refresh token al cerrar sesion
                    .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                    // Probes de liveness/readiness (Actuator, incluye chequeo de DB en readiness)
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/v1/businesses").permitAll()
                    // Assets estaticos (favicon, webjars, recursos empaquetados)
                    .requestMatchers(
                            "/favicon.ico",
                            "/static/**",
                            "/css/**",
                            "/js/**",
                            "/images/**",
                            "/webjars/**"
                    ).permitAll();

                if (isProdProfile()) {
                    // OpenAPI/Swagger bloqueado en produccion (defensa en profundidad con springdoc desactivado)
                    auth.requestMatchers(
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                    ).denyAll();
                } else {
                    auth.requestMatchers(
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
                    ).permitAll();
                }

                auth
                    .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                    .requestMatchers(HttpMethod.GET, "/v1/resources/**", "/api/resources/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "/v1/reservations", "/v1/reservations/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/v1/businesses").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/v1/businesses/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/v1/businesses/*/resources").authenticated()
                    .requestMatchers(HttpMethod.POST, "/v1/reservations").hasAnyRole("USER", "OWNER")
                    .requestMatchers(HttpMethod.PATCH, "/v1/reservations/**").hasAnyRole("USER", "OWNER")
                    .requestMatchers(HttpMethod.POST, "/api/reservations").hasAnyRole("USER", "OWNER")
                    .requestMatchers("/api/reservations/**").authenticated()
                    .anyRequest().authenticated();
            })
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
