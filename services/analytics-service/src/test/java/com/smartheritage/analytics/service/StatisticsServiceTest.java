package com.smartheritage.analytics.service;

import com.smartheritage.analytics.dto.StatisticsResponse;
import com.smartheritage.analytics.repository.StatisticsRepository;
import com.smartheritage.analytics.repository.StatisticsRepository.Filter;
import com.smartheritage.common.web.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private StatisticsRepository repository;

    @InjectMocks
    private StatisticsService service;

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 51})
    void topNgoaiKhoang_tra400(int top) {
        ApiException e = assertThrows(ApiException.class, () -> service.getOverview(top, null, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        verifyNoInteractions(repository);
    }

    @Test
    void tuNgaySauDenNgay_tra400() {
        ApiException e = assertThrows(ApiException.class, () -> service.getOverview(5, null,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 5)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        verifyNoInteractions(repository);
    }

    @Test
    void chuaCoDuLieu_soDem0VaTrungBinhNull() {
        Map<String, Object> visits = new HashMap<>();
        visits.put("total_views", 0L);
        visits.put("total_visitors", 0L);
        visits.put("avg_duration", null);
        Map<String, Object> ratings = new HashMap<>();
        ratings.put("total_count", 0L);
        ratings.put("avg_all", null);
        ratings.put("approved_count", BigDecimal.ZERO);
        ratings.put("avg_approved", null);
        when(repository.countViewsAndVisitors(any())).thenReturn(visits);
        when(repository.summarizeRatings(any())).thenReturn(ratings);

        StatisticsResponse r = service.getOverview(5, null, null, null);

        assertEquals(0, r.getTotalViews());
        assertEquals(0, r.getApprovedFeedbackCount());
        assertNull(r.getAverageDurationSeconds());
        assertNull(r.getAverageRatingApproved());
        verify(repository).countDailyViews(any(), eq(7));
    }

    @Test
    void coKhoangNgay_truyenBoLocVaVeToiDa31Ngay() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        Filter filter = new Filter(2L, from, to);
        when(repository.countViewsAndVisitors(filter)).thenReturn(Map.of("total_views", 3L, "total_visitors", 2L,
                "avg_duration", new BigDecimal("170.0")));
        when(repository.summarizeRatings(filter)).thenReturn(Map.of("total_count", 1L,
                "avg_all", new BigDecimal("5.00"), "approved_count", BigDecimal.ONE, "avg_approved", new BigDecimal("5.00")));
        when(repository.findTopArtifacts(filter, 5)).thenReturn(List.of());

        StatisticsResponse r = service.getOverview(5, 2L, from, to);

        assertEquals(3, r.getTotalViews());
        assertEquals(170.0, r.getAverageDurationSeconds());
        assertEquals(5.0, r.getAverageRatingApproved());
        verify(repository).countDailyViews(filter, 31);
        verify(repository).findRecentFeedbacks(eq(filter), anyInt());
    }

    @Test
    void realtime_diemTrungBinhLamTron2ChuSo() {
        Map<String, Object> sums = new HashMap<>();
        sums.put("views", new BigDecimal("12"));
        sums.put("feedbacks", new BigDecimal("3"));
        sums.put("rating_sum", new BigDecimal("13"));
        when(repository.sumRealtime(any(LocalDate.class), eq(1L))).thenReturn(sums);
        when(repository.findTopRealtime(any(LocalDate.class), eq(1L), anyInt())).thenReturn(List.of());

        var result = service.getRealtime(1L);

        assertEquals(LocalDate.now(), result.getDate());
        assertEquals(12, result.getViews());
        assertEquals(3, result.getFeedbacks());
        assertEquals(4.33, result.getAverageRating());
    }

    @Test
    void realtime_chuaCoDanhGia_diemNull() {
        Map<String, Object> sums = new HashMap<>();
        sums.put("views", 0L);
        sums.put("feedbacks", 0L);
        sums.put("rating_sum", 0L);
        when(repository.sumRealtime(any(LocalDate.class), any())).thenReturn(sums);
        when(repository.findTopRealtime(any(LocalDate.class), any(), anyInt())).thenReturn(List.of());

        assertNull(service.getRealtime(null).getAverageRating());
    }
}
