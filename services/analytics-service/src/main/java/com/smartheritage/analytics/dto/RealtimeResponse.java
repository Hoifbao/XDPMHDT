package com.smartheritage.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Số liệu "hôm nay" cho Dashboard (Tuần 6, Tuấn), đọc bảng rt_daily_counters
 * do sự kiện RabbitMQ cập nhật ngay, không chờ NiFi. Không cộng với số liệu của StatisticsResponse.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RealtimeResponse {

    /** Ngày đang đếm (theo giờ máy chạy analytics-service) */
    private LocalDate date;

    /** Số sự kiện visit.recorded trong ngày */
    private long views;

    /** Số sự kiện feedback.created trong ngày (mọi trạng thái) */
    private long feedbacks;

    /** Điểm TB của đánh giá mới trong ngày, làm tròn 2 chữ số; null nếu chưa có */
    private Double averageRating;

    /** Hiện vật nhiều lượt xem nhất trong ngày */
    private List<StatisticsResponse.TopArtifact> topArtifacts;
}
