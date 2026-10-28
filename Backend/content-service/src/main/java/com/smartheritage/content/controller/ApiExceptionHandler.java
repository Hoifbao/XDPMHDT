package com.smartheritage.content.controller;
import com.smartheritage.content.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<ApiResponse<Void>> status(ResponseStatusException e) {
  return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.error(e.getReason()));
 }
 @ExceptionHandler(DataIntegrityViolationException.class)
 public ResponseEntity<ApiResponse<Void>> conflict(DataIntegrityViolationException e) {
  return ResponseEntity.status(409).body(ApiResponse.error("Artifact code or beacon UUID already exists, or a field violates its database constraint"));
 }
}
