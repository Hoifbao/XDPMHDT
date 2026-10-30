package com.smartheritage.feedback.service;

import com.smartheritage.common.web.ApiException;
import com.smartheritage.feedback.client.ArtifactClient;
import com.smartheritage.feedback.client.UserProfileClient;
import com.smartheritage.feedback.dto.FeedbackRequest;
import com.smartheritage.feedback.dto.FeedbackResponse;
import com.smartheritage.feedback.dto.RatingSummaryResponse;
import com.smartheritage.feedback.entity.Feedback;
import com.smartheritage.feedback.repository.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private ArtifactClient artifactClient;
    @Mock
    private UserProfileClient userProfileClient;

    @InjectMocks
    private FeedbackService feedbackService;

    // ─────────────── createFeedback ───────────────

    @Test
    void createFeedback_hopLe_luuPendingVaChupTenNguoiGui() {
        when(artifactClient.getArtifactStatus(1L)).thenReturn("ACTIVE");
        when(userProfileClient.findCurrentUserFullName()).thenReturn(Optional.of("Nguyễn Văn An"));

        FeedbackResponse res = feedbackService.createFeedback(1L, 2L, new FeedbackRequest(5.0, "  Rất hay  "));

        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(2L);
        assertThat(saved.getValue().getArtifactId()).isEqualTo(1L);
        assertThat(saved.getValue().getReviewerName()).isEqualTo("Nguyễn Văn An");
        assertThat(saved.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(res.getRating()).isEqualTo(5);
        assertThat(res.getComment()).isEqualTo("Rất hay");
        assertThat(res.getReviewerName()).isEqualTo("Nguyễn Văn An");
    }

    @Test
    void createFeedback_authServiceLoi_vanLuuVoiTenKhach() {
        when(artifactClient.getArtifactStatus(1L)).thenReturn("ACTIVE");
        when(userProfileClient.findCurrentUserFullName()).thenReturn(Optional.empty());

        FeedbackResponse res = feedbackService.createFeedback(1L, 2L, new FeedbackRequest(4.0, "   "));

        verify(feedbackRepository).save(any(Feedback.class));
        assertThat(res.getReviewerName()).isEqualTo("Khách");
        assertThat(res.getComment()).isNull();
    }

    @ParameterizedTest
    @ValueSource(doubles = {4.5, 0, 6, -1, Double.NaN})
    void createFeedback_ratingSai_tra400(double rating) {
        assertStatus(() -> feedbackService.createFeedback(1L, 2L, new FeedbackRequest(rating, null)),
                HttpStatus.BAD_REQUEST);
        verifyNoInteractions(artifactClient, feedbackRepository);
    }

    @Test
    void createFeedback_thieuRatingHoacBody_tra400() {
        assertStatus(() -> feedbackService.createFeedback(1L, 2L, new FeedbackRequest(null, "x")),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> feedbackService.createFeedback(1L, 2L, null), HttpStatus.BAD_REQUEST);
    }

    @Test
    void createFeedback_binhLuanQua500KyTu_tra400() {
        String longComment = "a".repeat(501);
        assertStatus(() -> feedbackService.createFeedback(1L, 2L, new FeedbackRequest(3.0, longComment)),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void createFeedback_hienVatInactive_tra400() {
        when(artifactClient.getArtifactStatus(4L)).thenReturn("INACTIVE");
        assertStatus(() -> feedbackService.createFeedback(4L, 2L, new FeedbackRequest(3.0, null)),
                HttpStatus.BAD_REQUEST);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void createFeedback_hienVatKhongTonTai_tra404() {
        when(artifactClient.getArtifactStatus(99L)).thenThrow(ApiException.notFound("Không tìm thấy hiện vật"));
        assertStatus(() -> feedbackService.createFeedback(99L, 2L, new FeedbackRequest(3.0, null)),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void createFeedback_contentServiceTat_tra503() {
        when(artifactClient.getArtifactStatus(1L)).thenThrow(ApiException.serviceUnavailable("down"));
        assertStatus(() -> feedbackService.createFeedback(1L, 2L, new FeedbackRequest(3.0, null)),
                HttpStatus.SERVICE_UNAVAILABLE);
        verify(feedbackRepository, never()).save(any());
    }

    // ─────────────── getApprovedFeedbacks ───────────────

    @Test
    void getApprovedFeedbacks_chiLayApproved_tenNullThanhKhach() {
        when(artifactClient.getArtifactStatus(1L)).thenReturn("active");
        when(feedbackRepository.findByArtifactIdAndStatusOrderByCreatedAtDescIdDesc(1L, "APPROVED"))
                .thenReturn(List.of(feedback(1L, 5, "APPROVED", null)));

        List<FeedbackResponse> list = feedbackService.getApprovedFeedbacks(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getReviewerName()).isEqualTo("Khách");
    }

    @Test
    void getApprovedFeedbacks_hienVatInactive_tra404() {
        when(artifactClient.getArtifactStatus(4L)).thenReturn("INACTIVE");
        assertStatus(() -> feedbackService.getApprovedFeedbacks(4L), HttpStatus.NOT_FOUND);
    }

    // ─────────────── getRatingSummary ───────────────

    @Test
    void getRatingSummary_tinhTrungBinhLamTron1ChuSo() {
        // 5 sao x 2, 4 sao x 1  -> (10 + 4) / 3 = 4.666... -> 4.7
        when(feedbackRepository.countByRating(1L, "APPROVED"))
                .thenReturn(List.of(new Object[]{5, 2L}, new Object[]{4, 1L}));

        RatingSummaryResponse s = feedbackService.getRatingSummary(1L);

        assertThat(s.getTotalRatings()).isEqualTo(3);
        assertThat(s.getAverageRating()).isEqualTo(4.7);
        assertThat(s.getDistribution()).containsEntry(5, 2L).containsEntry(4, 1L).containsEntry(1, 0L).hasSize(5);
        verifyNoInteractions(artifactClient);
    }

    @Test
    void getRatingSummary_chuaCoDanhGia_trungBinhNull() {
        when(feedbackRepository.countByRating(3L, "APPROVED")).thenReturn(List.of());

        RatingSummaryResponse s = feedbackService.getRatingSummary(3L);

        assertThat(s.getTotalRatings()).isZero();
        assertThat(s.getAverageRating()).isNull();
    }

    // ─────────────── admin ───────────────

    @Test
    void getFeedbacksForAdmin_khongTruyenStatus_layTatCa() {
        when(feedbackRepository.findAllByOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(feedback(1L, 5, "APPROVED", "A"), feedback(2L, 3, "PENDING", "B")));

        assertThat(feedbackService.getFeedbacksForAdmin(" ")).hasSize(2);
    }

    @Test
    void getFeedbacksForAdmin_locTheoStatusKhongPhanBietHoaThuong() {
        when(feedbackRepository.findByStatusOrderByCreatedAtDescIdDesc("PENDING"))
                .thenReturn(List.of(feedback(3L, 3, "PENDING", "Trần Thị Mai")));

        assertThat(feedbackService.getFeedbacksForAdmin("pending")).hasSize(1);
    }

    @Test
    void getFeedbacksForAdmin_statusLa_tra400() {
        assertStatus(() -> feedbackService.getFeedbacksForAdmin("DELETED"), HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateStatus_approved_capNhat() {
        Feedback f = feedback(3L, 3, "PENDING", "Trần Thị Mai");
        when(feedbackRepository.findById(3L)).thenReturn(Optional.of(f));

        FeedbackResponse res = feedbackService.updateStatus(3L, " approved ");

        assertThat(res.getStatus()).isEqualTo("APPROVED");
        verify(feedbackRepository).save(f);
    }

    @Test
    void updateStatus_datLaiPending_tra400() {
        assertStatus(() -> feedbackService.updateStatus(3L, "PENDING"), HttpStatus.BAD_REQUEST);
        assertStatus(() -> feedbackService.updateStatus(3L, null), HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateStatus_khongTimThay_tra404() {
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());
        assertStatus(() -> feedbackService.updateStatus(99L, "REJECTED"), HttpStatus.NOT_FOUND);
    }

    // ─────────────── helpers ───────────────

    private static Feedback feedback(Long id, int rating, String status, String reviewerName) {
        return Feedback.builder().id(id).userId(2L).artifactId(1L).rating(rating)
                .status(status).reviewerName(reviewerName).build();
    }

    private static void assertStatus(Runnable call, HttpStatus expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(expected);
    }
}
