package org.example.java_ai.config;

import org.example.java_ai.mapper.RoleMapper;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @org.springframework.beans.factory.annotation.Value("${app.admin.user-ids:1}")
    private String adminUserIds;

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:*}")
    private String allowedOrigins;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.DISABLED))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;"))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
            )
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开端点
                .requestMatchers("/user/login", "/user/register", "/user/refresh").permitAll()
                .requestMatchers("/product/list", "/product/top-sales",
                        "/product/top-rated", "/product/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/product/**").permitAll()
                .requestMatchers("/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/category/**").permitAll()
                .requestMatchers("/cart/**").authenticated()
                .requestMatchers("/ai/customer-service/**").permitAll()
                .requestMatchers("/ai/recommend/**").permitAll()
                .requestMatchers("/upload/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/swagger-ui", "/swagger-ui/**",
                        "/v3/api-docs", "/v3/api-docs/**").permitAll()
                // 管理后台需要 ADMIN 角色
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 其余接口需要登录
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(RoleMapper roleMapper) {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(adminUserIds);
        filter.setRoleMapper(roleMapper);
        return filter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
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
