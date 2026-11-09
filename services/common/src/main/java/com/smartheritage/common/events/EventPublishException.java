package com.smartheritage.common.events;

/** RabbitMQ không nhận sự kiện (lỗi kết nối, bị từ chối, không có queue nhận, hết thời gian chờ). */
public class EventPublishException extends RuntimeException {

    public EventPublishException(String message) {
        super(message);
    }

    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
