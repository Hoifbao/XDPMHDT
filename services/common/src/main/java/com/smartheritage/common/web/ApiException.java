package com.smartheritage.common.web;

import org.springframework.http.HttpStatus;

/**
 * Ném trong Service/Controller để trả lỗi có mã HTTP rõ ràng, vd:
 * <pre>
 * throw ApiException.notFound("Không tìm thấy di tích id=" + id);
 * throw ApiException.badRequest("siteId không tồn tại");
 * </pre>
 * GlobalExceptionHandler sẽ chuyển thành JSON {success:false, message}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /** Dùng khi gọi service khác bị lỗi/timeout (vd content-service gọi site-service). */
    public static ApiException serviceUnavailable(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
