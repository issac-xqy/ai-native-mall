package org.example.java_ai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.mapper.RoleMapper;
import org.example.java_ai.util.TokenUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Set<Long> adminIds;
    private RoleMapper roleMapper;

    public JwtAuthenticationFilter(String adminUserIds) {
        this.adminIds = Arrays.stream(adminUserIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }

    /**
     * 延迟注入 RoleMapper（避免构造器参数变更影响 SecurityConfig）
     */
    public void setRoleMapper(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
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
                var authorities = resolveAuthorities(userId);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, token, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute("userId", userId);
                request.setAttribute("token", token);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 解析用户权限：优先从 DB 加载，降级到硬编码 admin ID
     */
    private List<SimpleGrantedAuthority> resolveAuthorities(Long userId) {
        // 硬编码管理员兜底
        boolean isAdmin = adminIds.contains(userId);

        // 从 DB 加载角色和权限
        Set<String> permCodes = new LinkedHashSet<>();
        Set<String> roleCodes = new LinkedHashSet<>();

        if (roleMapper != null) {
            try {
                List<String> dbPermissions = roleMapper.findPermissionsByUserId(userId);
                if (dbPermissions != null) permCodes.addAll(dbPermissions);
                List<String> dbRoles = roleMapper.findRoleCodesByUserId(userId);
                if (dbRoles != null) roleCodes.addAll(dbRoles);
            } catch (Exception e) {
                log.warn("从数据库加载权限失败，降级到硬编码模式: userId={}", userId, e);
            }
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 添加角色前缀
        for (String roleCode : roleCodes) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode.toUpperCase()));
        }
        if (isAdmin) {
            if (!roleCodes.contains("admin")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
        }
        if (!isAdmin && roleCodes.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // 添加权限码
        for (String perm : permCodes) {
            authorities.add(new SimpleGrantedAuthority(perm));
        }

        return authorities;
    }
}
