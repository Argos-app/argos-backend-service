package com.argos.backend_services.security;

import com.argos.backend_services.service.JwtService;
import com.argos.backend_services.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().equals("/auth/signin");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (token.isBlank() || !jwtService.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtService.extractClaims(token);
        UUID userId = UUID.fromString(claims.getSubject());
        var currentAccess = userRepository.findCurrentAccessById(userId);
        if (currentAccess.isEmpty()
                || !Boolean.TRUE.equals(currentAccess.get().getUserActive())
                || !Boolean.TRUE.equals(currentAccess.get().getPermissionActive())) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }
        String permission = currentAccess.get().getPermissionCode();
        String propertyId = claims.get("propertyId", String.class);
        CurrentUserData currentUser = new CurrentUserData(
                userId,
                claims.get("email", String.class),
                claims.get("name", String.class),
                permission,
                propertyId == null ? null : UUID.fromString(propertyId)
        );

        List<SimpleGrantedAuthority> authorities = permission != null
                ? List.of(new SimpleGrantedAuthority(permission))
                : List.of();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
