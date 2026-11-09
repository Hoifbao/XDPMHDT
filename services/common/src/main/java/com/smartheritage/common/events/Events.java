package com.smartheritage.common.events;

/** Hằng số dùng chung cho sự kiện RabbitMQ của cả nhóm (tuần 6). */
public final class Events {

    /** Exchange kiểu topic mà mọi service gửi sự kiện vào. Bên nhận tự tạo queue và bind theo routing key. */
    public static final String EXCHANGE = "sh.events";

    /** Tên metric Micrometer; trên Prometheus là sh_events_published_total{type, result}. */
    public static final String METRIC_PUBLISHED = "sh.events.published";

    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAILED = "failed";

    private Events() {
    }
}
