package com.smartheritage.feedback.controller;

import com.smartheritage.feedback.dto.FeedbackResponse;
import com.smartheritage.feedback.service.FeedbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Kiểm tra lớp web: đọc người gửi từ header X-User-*, mã HTTP và khung JSON {success, message, data}. */
@WebMvcTest({FeedbackController.class, AdminFeedbackController.class, RatingSummaryController.class})
class FeedbackControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

    @Test
    void postFeedback_khongCoHeaderNguoiDung_tra401() throws Exception {
        mockMvc.perform(post("/api/artifacts/1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(feedbackService);
    }

    @Test
    void postFeedback_coHeaderGateway_tra201VaDungUserIdTuHeader() throws Exception {
        when(feedbackService.submitFeedback(eq(1L), eq(2L), any()))
                .thenReturn(new FeedbackService.SubmitResult(FeedbackResponse.builder().id(7L).artifactId(1L).rating(5)
                        .reviewerName("Nguyễn Văn An").status("PENDING").build(), true));

        mockMvc.perform(post("/api/artifacts/1/feedbacks")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "VISITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        // userId trong body (nếu client cố gửi) bị bỏ qua
                        .content("{\"rating\":5,\"userId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        verify(feedbackService).submitFeedback(eq(1L), eq(2L), any());
    }

    @Test
    void postFeedback_daCoDanhGia_tra200() throws Exception {
        when(feedbackService.submitFeedback(eq(1L), eq(2L), any()))
                .thenReturn(new FeedbackService.SubmitResult(FeedbackResponse.builder().id(1L).artifactId(1L).rating(2)
                        .status("PENDING").build(), false));

        mockMvc.perform(post("/api/artifacts/1/feedbacks")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", "VISITOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void updateStatus_nhanCaPutVaPatch() throws Exception {
        when(feedbackService.updateStatus(3L, "APPROVED"))
                .thenReturn(FeedbackResponse.builder().id(3L).status("APPROVED").build());

        mockMvc.perform(put("/api/admin/feedbacks/3/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        mockMvc.perform(patch("/api/admin/feedbacks/3/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void bodySaiDinhDang_tra400() throws Exception {
        mockMvc.perform(put("/api/admin/feedbacks/3/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
