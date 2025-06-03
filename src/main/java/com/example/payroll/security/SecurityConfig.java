package com.example.payroll.security;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService     = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1) Disable CSRF (we rely on stateless JWTs, so no sessions):
            .csrf(csrf -> csrf.disable())

            // 2) Configure public vs. protected URLs:
            .authorizeHttpRequests(auth -> auth

                // ─────────── PUBLIC (no JWT required) ───────────
                .requestMatchers("/auth/**").permitAll()         // login/register
                .requestMatchers("/actuator/**").permitAll()     // EXPOSE ALL actuator endpoints
                .requestMatchers("/error").permitAll()           // allow default Spring Boot /error
                .requestMatchers("/swagger-ui.html").permitAll() // OpenAPI/Swagger
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/api-docs-ui/**").permitAll()
                .requestMatchers("/api-docs-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/v3/api-docs.yaml").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                .requestMatchers("/employees/**").permitAll()    // your public REST routes
                .requestMatchers("/departments/**").permitAll()

                // ─────────── PROTECTED (JWT required) ───────────
                .requestMatchers("/admin/**").hasRole("ADMIN")   // only ADMIN can read /admin/**
                .anyRequest().authenticated()                    // everything else needs a JWT
            )

            // 3) Stateless session (no HttpSession):
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4) Use our DaoAuthenticationProvider (CustomUserDetailsService + BCryptPasswordEncoder):
            .authenticationProvider(authenticationProvider());

        // 5) Insert our JWT filter before Spring’s UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ───────── Beans for authentication manager & BCrypt ─────────
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(Collections.singletonList(provider));
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
