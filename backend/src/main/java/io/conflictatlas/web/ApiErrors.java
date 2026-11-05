package io.conflictatlas.web;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public final class ApiErrors {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> invalid(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "message",
                "Check the scenario inputs. Days: 7–180; trials: 100–2000; readiness/supply:"
                    + " 20–100; other sliders: 0–100; title: 1–80 characters."));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<?> unreadable() {
    return ResponseEntity.badRequest().body(Map.of("message", "Invalid JSON or scenario values."));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(
            Map.of(
                "message",
                e.getReason() == null ? "Request could not be completed." : e.getReason()));
  }
}
