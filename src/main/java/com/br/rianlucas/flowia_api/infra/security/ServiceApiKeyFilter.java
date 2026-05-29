package com.br.rianlucas.flowia_api.infra.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ServiceApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Service-Key";

    @Value("${service.api-key}")
    private String serviceApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("POST".equals(request.getMethod()) && "/analysis".equals(request.getServletPath())) {
            String providedKey = request.getHeader(API_KEY_HEADER);
            if (providedKey != null && isValidKey(providedKey)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "service", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    // Comparacao em tempo constante — previne timing attacks
    private boolean isValidKey(String providedKey) {
        return MessageDigest.isEqual(
                serviceApiKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8));
    }
}
