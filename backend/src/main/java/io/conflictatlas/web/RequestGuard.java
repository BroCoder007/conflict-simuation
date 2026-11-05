package io.conflictatlas.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public final class RequestGuard extends OncePerRequestFilter {
  private final Map<String, ArrayDeque<Long>> attempts = new HashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
    response.setHeader(
        "Content-Security-Policy",
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self'"
            + " data: blob:; connect-src 'self'; worker-src 'self' blob:; font-src 'self';"
            + " frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
    response.setHeader(
        "Cache-Control",
        request.getRequestURI().startsWith("/assets/")
            ? "public, max-age=31536000, immutable"
            : "private, no-store");
    if (Set.of("POST", "DELETE", "PUT", "PATCH").contains(request.getMethod())
        && request.getRequestURI().startsWith("/api/")) {
      String origin = request.getHeader("Origin");
      String self = request.getScheme() + "://" + request.getHeader("Host");
      try {
        if (origin == null || !URI.create(origin).equals(URI.create(self))) {
          response.sendError(403, "A same-origin request is required.");
          return;
        }
      } catch (IllegalArgumentException ex) {
        response.sendError(403);
        return;
      }
      if (request.getContentLengthLong() > 65536) {
        response.sendError(413);
        return;
      }
      if (!allow(request.getRemoteAddr())) {
        response.setHeader("Retry-After", "60");
        response.sendError(429, "Please wait before trying again.");
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private synchronized boolean allow(String ip) {
    long cutoff = Instant.now().minusSeconds(60).toEpochMilli();
    attempts
        .entrySet()
        .removeIf(entry -> {
          entry.getValue().removeIf(t -> t < cutoff);
          return entry.getValue().isEmpty();
        });
    if (!attempts.containsKey(ip) && attempts.size() >= 10000) return false;
    var times = attempts.computeIfAbsent(ip, k -> new ArrayDeque<>());
    if (times.size() >= 20) return false;
    times.add(Instant.now().toEpochMilli());
    return true;
  }
}
