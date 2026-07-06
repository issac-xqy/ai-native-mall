package org.example.java_ai.config;

import org.example.java_ai.util.TokenUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @org.springframework.beans.factory.annotation.Value("${app.admin.user-ids:1}")
    private String adminUserIds;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开端点
                .requestMatchers("/api/user/login", "/api/user/register").permitAll()
                .requestMatchers("/api/product/list", "/api/product/top-sales",
                        "/api/product/top-rated", "/api/product/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/product/**").permitAll()
                .requestMatchers("/api/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/category/**").permitAll()
                .requestMatchers("/api/cart/**").authenticated()
                .requestMatchers("/api/ai/customer-service/**").permitAll()
                .requestMatchers("/api/ai/recommend/**").permitAll()
                .requestMatchers("/api/upload/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/swagger-ui", "/swagger-ui/**",
                        "/v3/api-docs", "/v3/api-docs/**").permitAll()
                // 管理后台需要 ADMIN 角色
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 其余接口需要登录
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(adminUserIds);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        config.setExposedHeaders(List.of("Content-Type", "Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
