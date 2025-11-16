package com.skrepta.skreptajava.security;

import com.skrepta.skreptajava.auth.service.JwtAuthenticationFilter;
import com.skrepta.skreptajava.auth.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC endpoints
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()
                        
                        // Swagger/OpenAPI documentation
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        
                        // Health check and error pages
                        .requestMatchers("/", "/error").permitAll()
                        
                        // READ-ONLY PUBLIC endpoints
                        .requestMatchers(
                                "GET",
                                "/api/shops",
                                "/api/shops/{id}",
                                "/api/shops/{shopId}/items",
                                "/api/items",
                                "/api/items/{id}",
                                "/api/categories"
                        ).permitAll()
                        
                        // AUTHENTICATED USER endpoints
                        // Favorites
                        .requestMatchers("/api/favorites/**").authenticated()

                        // ✅ НОВОЕ: Публичный эндпоинт для увеличения просмотров
                        .requestMatchers("POST", "/api/items/{id}/view").permitAll()

                        .requestMatchers("/api/shop-favorites/**").authenticated()
                        
                        // ✅ ДОБАВЬТЕ ЭТУ СТРОКУ - Удаление своего аккаунта
                        .requestMatchers("DELETE", "/api/auth/me").authenticated()
                        
                        // Refresh token
                        .requestMatchers("/api/auth/refresh-token").authenticated()
                        
                        // ADMIN endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        
                        // SHOP OWNER / ADMIN endpoints - управление магазинами (доступно владельцу магазина и ADMIN)
                        .requestMatchers("POST", "/api/shops").authenticated()
                        .requestMatchers("PUT", "/api/shops/{id}").authenticated()
                        .requestMatchers("DELETE", "/api/shops/{id}").authenticated()
                        
                        // SHOP OWNER / ADMIN endpoints - управление товарами (доступно владельцу магазина и ADMIN)
                        .requestMatchers("POST", "/api/shops/{shopId}/items").authenticated()
                        .requestMatchers("PUT", "/api/items/{id}").authenticated()
                        .requestMatchers("DELETE", "/api/items/{id}").authenticated()
                        
                        // ADMIN endpoints - управление категориями
                        .requestMatchers("POST", "/api/categories").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/categories/{id}").hasRole("ADMIN")
                        .requestMatchers("PATCH", "/api/categories/{id}/status").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/api/categories/{id}").hasRole("ADMIN")
                        
                        // ✅ НОВОЕ: Загрузка иконки категории (только ADMIN)
                        .requestMatchers("POST", "/api/categories/{id}/icon").hasRole("ADMIN")
                        
                        // Все остальные запросы требуют аутентификации
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}