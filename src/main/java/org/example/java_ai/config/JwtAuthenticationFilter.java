package org.example.java_ai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.util.TokenUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Set<Long> adminIds;

    public JwtAuthenticationFilter(String adminUserIds) {
        this.adminIds = Arrays.stream(adminUserIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }

        if (token != null && !token.isEmpty() && TokenUtil.isTokenValid(token)) {
            Long userId = TokenUtil.parseUserId(token);
            if (userId != null) {
                var authorities = adminIds.contains(userId)
                        ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, token, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute("userId", userId);
                request.setAttribute("token", token);
            }
        }

        filterChain.doFilter(request, response);
    }
}
