package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.FeedbackRequest;
import com.smartheritage.backend.dto.FeedbackResponse;
import com.smartheritage.backend.entity.Artifact;
import com.smartheritage.backend.entity.Feedback;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.FeedbackRepository;
import com.smartheritage.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test FeedbackService (Bảo W3, bổ sung thay Tường): rating, bình luận, trạng thái hiện vật, duyệt.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {
    @Mock FeedbackRepository feedbackRepository;
    @Mock ArtifactRepository artifactRepository;
    @Mock UserRepository userRepository;
    @InjectMocks FeedbackService feedbackService;

    private static FeedbackRequest request(Double rating, String comment) {
        FeedbackRequest r = new FeedbackRequest();
        r.setRating(rating);
        r.setComment(comment);
        return r;
    }

    @Test
    void createFeedbackIsPendingWithTrimmedComment() {
        when(artifactRepository.findById(1L)).thenReturn(Optional.of(Artifact.builder().id(1L).status("ACTIVE").build()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).username("visitor_an").fullName("Nguyễn An").build()));

        FeedbackResponse res = feedbackService.createFeedback(1L, 2L, request(5.0, "  Rất hay  "));

        assertEquals("PENDING", res.getStatus());
        assertEquals(5, res.getRating());
        assertEquals("Rất hay", res.getComment());
        assertEquals("Nguyễn An", res.getReviewerName());
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void decimalRatingIsRejectedBeforeLoadingArtifact() {
        assertThrows(IllegalArgumentException.class,
                () -> feedbackService.createFeedback(1L, 2L, request(4.5, null)));
        verifyNoInteractions(artifactRepository, feedbackRepository);
    }

    @Test
    void ratingOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> feedbackService.createFeedback(1L, 2L, request(6.0, null)));
    }

    @Test
    void inactiveArtifactCannotReceiveFeedback() {
        when(artifactRepository.findById(1L)).thenReturn(Optional.of(Artifact.builder().id(1L).status("INACTIVE").build()));
        assertThrows(IllegalArgumentException.class,
                () -> feedbackService.createFeedback(1L, 2L, request(4.0, "ok")));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void missingArtifactIsNotFound() {
        when(artifactRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class,
                () -> feedbackService.createFeedback(99L, 2L, request(4.0, null)));
    }

    @Test
    void adminCanApproveFeedback() {
        Feedback feedback = Feedback.builder().id(7L).artifactId(1L).rating(4).status("PENDING").build();
        when(feedbackRepository.findById(7L)).thenReturn(Optional.of(feedback));

        FeedbackResponse res = feedbackService.updateStatus(7L, " approved ");

        assertEquals("APPROVED", res.getStatus());
        verify(feedbackRepository).save(feedback);
    }

    @Test
    void statusCannotBeResetToPending() {
        assertThrows(IllegalArgumentException.class, () -> feedbackService.updateStatus(7L, "PENDING"));
        verifyNoInteractions(feedbackRepository);
    }
}
