package com.smartheritage.beacon.service;

import com.smartheritage.beacon.client.SiteClient;
import com.smartheritage.beacon.config.NotificationRabbitConfig;
import com.smartheritage.beacon.entity.Device;
import com.smartheritage.beacon.events.BeaconBatteryLowData;
import com.smartheritage.beacon.events.FeedbackApprovedData;
import com.smartheritage.beacon.repository.DeviceRepository;
import com.smartheritage.common.events.EventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test logic NotificationEventListener của beacon-service (Khoa - W6): nhận sự kiện
 * FeedbackApproved, chống trùng theo eventId, gửi FCM cho mọi thiết bị của du khách.
 * ProcessedEvents / DeviceRepository / FcmService đều mock - không cần RabbitMQ hay Firebase thật.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationEventListenerTest {

    private static final Long USER_ID = 7L;

    @Mock ProcessedEvents processedEvents;
    @Mock DeviceRepository deviceRepository;
    @Mock FcmService fcmService;
    @Mock SiteClient siteClient;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(processedEvents, deviceRepository, fcmService, siteClient);
        when(processedEvents.markIfNew(org.mockito.ArgumentMatchers.any(UUID.class), anyString())).thenReturn(true);
        when(deviceRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of(
                Device.builder().id(1L).userId(USER_ID).fcmToken("token-1").platform("android").build(),
                Device.builder().id(2L).userId(USER_ID).fcmToken("token-2").platform("android").build()));
        when(fcmService.sendToToken(anyString(), anyString(), anyString())).thenReturn(true);
    }

    private EventEnvelope<FeedbackApprovedData> approved(String artifactName) {
        return EventEnvelope.of(UUID.randomUUID(), "feedback.approved", Instant.now(),
                new FeedbackApprovedData(11L, USER_ID, 22L, artifactName));
    }

    // ─────────────── luồng vui ───────────────

    @Test
    void sendsToEveryDeviceWithArtifactName() {
        listener.onFeedbackApproved(approved("Bia Tiến sĩ"));

        verify(fcmService).sendToToken("token-1", NotificationEventListener.NOTIFICATION_TITLE,
                "Đánh giá của bạn về Bia Tiến sĩ đã được duyệt");
        verify(fcmService).sendToToken("token-2", NotificationEventListener.NOTIFICATION_TITLE,
                "Đánh giá của bạn về Bia Tiến sĩ đã được duyệt");
    }

    @Test
    void missingArtifactNameFallsBackToHienVatId() {
        listener.onFeedbackApproved(approved(null));

        verify(fcmService).sendToToken(eq("token-1"), anyString(),
                eq("Đánh giá của bạn về Hiện vật #22 đã được duyệt"));
    }

    @Test
    void blankArtifactNameFallsBackToo() {
        listener.onFeedbackApproved(approved("   "));

        verify(fcmService).sendToToken(eq("token-1"), anyString(),
                eq("Đánh giá của bạn về Hiện vật #22 đã được duyệt"));
    }

    // ─────────────── chống trùng / tin lỗi ───────────────

    @Test
    void duplicateEventIsSkippedWithoutFcm() {
        when(processedEvents.markIfNew(org.mockito.ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(false);

        listener.onFeedbackApproved(approved("Bia Tiến sĩ"));

        verify(fcmService, never()).sendToToken(anyString(), anyString(), anyString());
    }

    @Test
    void eventWithoutUserIdGoesToDlq() {
        EventEnvelope<FeedbackApprovedData> broken = EventEnvelope.of(UUID.randomUUID(),
                "feedback.approved", Instant.now(), new FeedbackApprovedData(11L, null, 22L, "Bia"));

        assertThatThrownBy(() -> listener.onFeedbackApproved(broken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thiếu data hoặc userId");
        verify(fcmService, never()).sendToToken(anyString(), anyString(), anyString());
    }

    @Test
    void userWithoutDevicesSendsNothing() {
        when(deviceRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of());

        listener.onFeedbackApproved(approved("Bia Tiến sĩ"));

        verify(fcmService, never()).sendToToken(anyString(), anyString(), anyString());
    }

    // ─────────────── tên thông báo ───────────────

    @Test
    void resolveArtifactNameUsesRule() {
        assertThat(NotificationEventListener.resolveArtifactName(
                new FeedbackApprovedData(1L, 2L, 9L, null))).isEqualTo("Hiện vật #9");
        assertThat(NotificationEventListener.resolveArtifactName(
                new FeedbackApprovedData(1L, 2L, 9L, ""))).isEqualTo("Hiện vật #9");
        assertThat(NotificationEventListener.resolveArtifactName(
                new FeedbackApprovedData(1L, 2L, 9L, "Đoạn Môn"))).isEqualTo("Đoạn Môn");
    }

    // ─────────────── W6: beacon.battery.low -> FCM topic Admin ───────────────

    private EventEnvelope<BeaconBatteryLowData> batteryLow(Long beaconId, Long siteId, Integer level) {
        return EventEnvelope.of(UUID.randomUUID(), "beacon.battery.low", Instant.now(),
                new BeaconBatteryLowData(beaconId, siteId, level, Instant.now()));
    }

    @Test
    void batteryLowSendsFcmToAdminTopicWithSiteName() {
        when(siteClient.findName(1L)).thenReturn(java.util.Optional.of("Khuê Văn Các"));

        listener.onBeaconBatteryLow(batteryLow(4L, 1L, 15));

        verify(fcmService).sendToTopic("admins", NotificationEventListener.NOTIFICATION_TITLE,
                "Pin beacon #4 tại Khuê Văn Các còn 15% — cần sạc hoặc thay pin");
    }

    @Test
    void batteryLowUnknownSiteFallsBackToSiteId() {
        when(siteClient.findName(9L)).thenReturn(java.util.Optional.empty());

        listener.onBeaconBatteryLow(batteryLow(7L, 9L, 8));

        verify(fcmService).sendToTopic("admins", NotificationEventListener.NOTIFICATION_TITLE,
                "Pin beacon #7 tại di tích #9 còn 8% — cần sạc hoặc thay pin");
    }

    @Test
    void batteryLowDuplicateEventIsSkipped() {
        when(processedEvents.markIfNew(org.mockito.ArgumentMatchers.any(UUID.class), anyString()))
                .thenReturn(false);

        listener.onBeaconBatteryLow(batteryLow(4L, 1L, 15));

        verify(fcmService, never()).sendToTopic(anyString(), anyString(), anyString());
    }

    @Test
    void batteryLowWithoutBeaconIdGoesToDlq() {
        EventEnvelope<BeaconBatteryLowData> broken = EventEnvelope.of(UUID.randomUUID(),
                "beacon.battery.low", Instant.now(), new BeaconBatteryLowData(null, 1L, 10, Instant.now()));

        assertThatThrownBy(() -> listener.onBeaconBatteryLow(broken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thiếu data");
        verify(fcmService, never()).sendToTopic(anyString(), anyString(), anyString());
    }
}
