package com.smartheritage.common.security;

/**
 * Tên header Gateway gắn khi chuyển request xuống service (sau khi đã kiểm tra JWT).
 * Gateway luôn xóa các header X-User-* do client tự gửi lên, nên service tin được giá trị này.
 */
public final class UserHeaders {
    public static final String PREFIX = "X-User-";
    public static final String USER_ID = "X-User-Id";
    /** Username đã mã hóa URL (UTF-8) để an toàn khi có ký tự đặc biệt. */
    public static final String USER_NAME = "X-User-Name";
    public static final String USER_ROLE = "X-User-Role";

    private UserHeaders() {
    }
}
