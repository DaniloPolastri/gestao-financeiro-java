package com.findash.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class CompanyContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String companyIdHeader = request.getHeader("X-Company-Id");
            if (companyIdHeader != null && !companyIdHeader.isBlank()) {
                CompanyContextHolder.set(UUID.fromString(companyIdHeader));
            }
            filterChain.doFilter(request, response);
        } finally {
            CompanyContextHolder.clear();
        }
    }
}
