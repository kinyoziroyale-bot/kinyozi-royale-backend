package com.kinyozi.royale.exception;
import com.kinyozi.royale.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> nf(NotFoundException e){
    return ResponseEntity.status(404).body(ApiError.of(404,"Not Found",e.getMessage()));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> br(BadRequestException e){
    return ResponseEntity.badRequest().body(ApiError.of(400,"Bad Request",e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e){
    String msg = e.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
        .orElse("Invalid request");
    return ResponseEntity.badRequest().body(ApiError.of(400,"Bad Request", msg));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> bc(BadCredentialsException e){
    return ResponseEntity.status(401).body(ApiError.of(401,"Unauthorized","Invalid credentials"));
  }

  /** Cross-tenant / ownership violations from services. Treated as 404 to avoid leaking existence. */
  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiError> sec(SecurityException e){
    log.warn("Access denied: {}", e.getMessage());
    return ResponseEntity.status(404).body(ApiError.of(404,"Not Found","Resource not found"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> denied(AccessDeniedException e){
    return ResponseEntity.status(403).body(
        ApiError.of(403,"Forbidden","You do not have permission to perform this action"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> rse(ResponseStatusException e){
    int s = e.getStatusCode().value();
    String reason = e.getReason() == null ? "Request failed" : e.getReason();
    return ResponseEntity.status(s).body(ApiError.of(s, HttpStatus.valueOf(s).getReasonPhrase(), reason));
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ApiError> dae(DataAccessException e){
    log.error("Database error", e);
    return ResponseEntity.status(500).body(
        ApiError.of(500,"Server Error","A database error occurred. Please try again."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> ex(Exception e){
    log.error("Unhandled server error", e);
    return ResponseEntity.status(500).body(
        ApiError.of(500,"Server Error","An unexpected error occurred. Please try again."));
  }
}
