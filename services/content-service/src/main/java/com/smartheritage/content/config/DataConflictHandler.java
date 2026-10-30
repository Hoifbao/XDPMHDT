package com.smartheritage.content.config;

import com.smartheritage.common.api.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Trùng mã hiện vật / beacon UUID / ngôn ngữ ở tầng DB -> 409 thay vì 500.
 * Chạy trước GlobalExceptionHandler (module common) nhờ @Order.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataConflictHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(
                "Mã hiện vật, beacon UUID hoặc ngôn ngữ đã tồn tại, hoặc dữ liệu vi phạm ràng buộc!"));
    }
}
