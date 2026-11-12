package com.smartheritage.beacon.service;

import com.smartheritage.beacon.client.ArtifactClient;
import com.smartheritage.beacon.client.SiteClient;
import com.smartheritage.beacon.dto.IBeaconAssignRequest;
import com.smartheritage.beacon.dto.IBeaconRequest;
import com.smartheritage.beacon.dto.IBeaconResponse;
import com.smartheritage.beacon.entity.IBeacon;
import com.smartheritage.beacon.repository.IBeaconRepository;
import com.smartheritage.common.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test logic IBeaconService của beacon-service (chuyển từ Backend cũ - Sprint 3).
 * Repository mock bằng Mockito; site/artifact giả bằng mock của SiteClient/ArtifactClient
 * (khi thật sẽ là REST gọi site-service / content-service - Xem SiteClientTest của content-service).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IBeaconServiceTest {

    private static final String SITE_NAME = "Văn Miếu - Quốc Tử Giám";
    private static final String ARTIFACT_NAME = "Bia Tiến sĩ";

    @Mock IBeaconRepository beaconRepository;
    @Mock SiteClient siteClient;
    @Mock ArtifactClient artifactClient;

    private IBeaconService service;

    @BeforeEach
    void setUp() {
        service = new IBeaconService(beaconRepository, siteClient, artifactClient);
        // API đọc không bắt service ngoài phải chạy: lỗi -> tên rỗng
        lenient().when(siteClient.findName(anyLong())).thenReturn(Optional.empty());
        lenient().when(artifactClient.findName(anyLong())).thenReturn(Optional.empty());
        lenient().when(siteClient.requireSite(anyLong())).thenReturn(new SiteClient.Site(1L, SITE_NAME));
    }

    private IBeacon beacon(Long id, Long siteId, Long artifactId, String status) {
        return IBeacon.builder()
                .id(id)
                .siteId(siteId)
                .artifactId(artifactId)
                .uuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0")
                .major(1)
                .minor(1)
                .status(status)
                .build();
    }

    private IBeaconRequest request(String uuid, Integer major, Integer minor,
                                   Long siteId, Long artifactId) {
        IBeaconRequest request = new IBeaconRequest();
        request.setUuid(uuid);
        request.setMajor(major);
        request.setMinor(minor);
        request.setSiteId(siteId);
        request.setArtifactId(artifactId);
        return request;
    }

    // ─────────────── registry ───────────────

    @Test
    void registryReturnsOnlyActiveBeacons() {
        IBeacon active = beacon(1L, 1L, 2L, "ACTIVE");
        IBeacon inactive = beacon(4L, 1L, null, "INACTIVE");
        when(beaconRepository.findByStatus("ACTIVE")).thenReturn(List.of(active));

        List<IBeaconResponse> result = service.getRegistry();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(result.get(0).getArtifactId()).isEqualTo(2L);
    }

    @Test
    void registryIsEmptyWhenNoActiveBeacons() {
        when(beaconRepository.findByStatus("ACTIVE")).thenReturn(List.of());

        assertThat(service.getRegistry()).isEmpty();
    }

    // ─────────────── assign ───────────────

    @Test
    void assignArtifactFromSameSiteIsSaved() {
        IBeacon beacon = beacon(1L, 1L, null, "ACTIVE");
        when(beaconRepository.findById(1L)).thenReturn(Optional.of(beacon));
        when(artifactClient.requireArtifact(2L))
                .thenReturn(new ArtifactClient.Artifact(2L, ARTIFACT_NAME, 1L));
        lenient().when(artifactClient.findName(2L)).thenReturn(Optional.of(ARTIFACT_NAME));

        IBeaconAssignRequest body = new IBeaconAssignRequest();
        body.setArtifactId(2L);
        IBeaconResponse response = service.assignArtifact(1L, body.getArtifactId());

        assertThat(response.getArtifactId()).isEqualTo(2L);
        assertThat(response.getArtifactName()).isEqualTo(ARTIFACT_NAME);
        assertThat(response.getSiteId()).isEqualTo(1L);
        verify(beaconRepository).save(beacon);
    }

    @Test
    void assignArtifactFromOtherSiteIsRejected() {
        IBeacon beacon = beacon(1L, 1L, null, "ACTIVE");
        when(beaconRepository.findById(1L)).thenReturn(Optional.of(beacon));
        when(artifactClient.requireArtifact(3L))
                .thenReturn(new ArtifactClient.Artifact(3L, "Đoan Môn", 2L));

        IBeaconAssignRequest body = new IBeaconAssignRequest();
        body.setArtifactId(3L);
        assertThatThrownBy(() -> service.assignArtifact(1L, body.getArtifactId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("không thuộc di tích");
        verify(beaconRepository, never()).save(any());
    }

    @Test
    void assignNullUnassignsArtifact() {
        IBeacon beacon = beacon(2L, 1L, 2L, "ACTIVE");
        when(beaconRepository.findById(2L)).thenReturn(Optional.of(beacon));

        IBeaconResponse response = service.assignArtifact(2L, null);

        assertThat(response.getArtifactId()).isNull();
        assertThat(response.getArtifactName()).isNull();
        verify(beaconRepository).save(beacon);
    }

    @Test
    void assignToMissingBeaconThrowsNotFound() {
        when(beaconRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignArtifact(99L, 2L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Không tìm thấy iBeacon với ID: 99");
    }

    @Test
    void assignMissingArtifactIsRejected() {
        IBeacon beacon = beacon(1L, 1L, null, "ACTIVE");
        when(beaconRepository.findById(1L)).thenReturn(Optional.of(beacon));
        when(artifactClient.requireArtifact(99L))
                .thenThrow(ApiException.badRequest("Không tìm thấy hiện vật với ID: 99"));

        assertThatThrownBy(() -> service.assignArtifact(1L, 99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Không tìm thấy hiện vật với ID: 99");
        verify(beaconRepository, never()).save(any());
    }

    // ─────────────── create ───────────────

    @Test
    void createNormalizesUuidAndDefaultsStatus() {
        IBeaconRequest request = request(
                "e2c56db5-dffb-48d2-b060-d0f5a71096e0", 1, 3, 1L, null);
        when(beaconRepository.existsByUuidAndMajorAndMinor(any(), any(), any())).thenReturn(false);

        IBeaconResponse response = service.createBeacon(request);

        assertThat(response.getUuid()).isEqualTo("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getSiteId()).isEqualTo(1L);
        verify(beaconRepository).save(any(IBeacon.class));
    }

    @Test
    void createRejectsDuplicate() {
        IBeaconRequest request = request(
                "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0", 1, 1, 1L, null);
        when(beaconRepository.existsByUuidAndMajorAndMinor(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.createBeacon(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    void createRejectsInvalidUuid() {
        IBeaconRequest request = request("not-a-uuid", 1, 3, 1L, null);

        assertThatThrownBy(() -> service.createBeacon(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("UUID không đúng định dạng");
    }

    @Test
    void createRejectsMajorOutOfRange() {
        IBeaconRequest request = request(
                "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0", 70000, 1, 1L, null);

        assertThatThrownBy(() -> service.createBeacon(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("0–65535");
    }

    // ─────────────── position (W5, cho Ân) ───────────────

    @Test
    void updatePositionSavesCoordinates() {
        IBeacon beacon = beacon(1L, 1L, 2L, "ACTIVE");
        when(beaconRepository.findById(1L)).thenReturn(Optional.of(beacon));

        IBeaconResponse response = service.updatePosition(1L, 12.5, 30.0);

        assertThat(response.getPosX()).isEqualTo(12.5);
        assertThat(response.getPosY()).isEqualTo(30.0);
        verify(beaconRepository).save(beacon);
    }

    @Test
    void updatePositionAllowsNullToClearCoordinates() {
        IBeacon beacon = IBeacon.builder()
                .id(2L).siteId(1L).uuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0")
                .major(1).minor(2).posX(25.0).posY(42.5).status("ACTIVE")
                .build();
        when(beaconRepository.findById(2L)).thenReturn(Optional.of(beacon));

        IBeaconResponse response = service.updatePosition(2L, null, null);

        assertThat(response.getPosX()).isNull();
        assertThat(response.getPosY()).isNull();
        verify(beaconRepository).save(beacon);
    }

    @Test
    void updatePositionToMissingBeaconThrowsNotFound() {
        when(beaconRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePosition(99L, 10.0, 20.0))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Không tìm thấy iBeacon với ID: 99");
        verify(beaconRepository, never()).save(any());
    }

    // ─────────────── battery (W6) ───────────────

    @Test
    void updateBatterySavesLevel() {
        IBeacon beacon = beacon(1L, 1L, 2L, "ACTIVE");
        when(beaconRepository.findById(1L)).thenReturn(Optional.of(beacon));

        IBeaconResponse response = service.updateBattery(1L, 15);

        assertThat(response.getBatteryLevel()).isEqualTo(15);
        verify(beaconRepository).save(beacon);
    }

    @Test
    void updateBatteryLowKeepsAlertFlag() {
        // Pin < 20: không đụng cờ battery_alerted_at (quyết định cảnh báo thuộc BatteryAlertService)
        IBeacon beacon = IBeacon.builder()
                .id(4L).siteId(1L).uuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0")
                .major(1).minor(4).batteryLevel(87).status("ACTIVE")
                .batteryAlertedAt(java.time.LocalDateTime.now())
                .build();
        when(beaconRepository.findById(4L)).thenReturn(Optional.of(beacon));

        service.updateBattery(4L, 15);

        assertThat(beacon.getBatteryAlertedAt()).isNotNull();
        verify(beaconRepository).save(beacon);
    }

    @Test
    void updateBatteryRecoveredClearsAlertFlag() {
        // Pin hồi lại >= 20% (sạc/thay pin): xóa cờ ngay để lần sắp cạn tiếp theo vẫn được báo
        IBeacon beacon = IBeacon.builder()
                .id(4L).siteId(1L).uuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0")
                .major(1).minor(4).batteryLevel(15).status("ACTIVE")
                .batteryAlertedAt(java.time.LocalDateTime.now())
                .build();
        when(beaconRepository.findById(4L)).thenReturn(Optional.of(beacon));

        service.updateBattery(4L, 95);

        assertThat(beacon.getBatteryLevel()).isEqualTo(95);
        assertThat(beacon.getBatteryAlertedAt()).isNull();
        verify(beaconRepository).save(beacon);
    }

    @Test
    void updateBatteryRejectsOutOfRange() {
        assertThatThrownBy(() -> service.updateBattery(1L, 101))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("0–100");
        assertThatThrownBy(() -> service.updateBattery(1L, -1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("0–100");
        assertThatThrownBy(() -> service.updateBattery(1L, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("0–100");
        verify(beaconRepository, never()).save(any());
    }

    @Test
    void updateBatteryToMissingBeaconThrowsNotFound() {
        when(beaconRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBattery(99L, 50))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Không tìm thấy iBeacon với ID: 99");
        verify(beaconRepository, never()).save(any());
    }

    // ─────────────── cross-service check ───────────────

    @Test
    void createRejectsSiteMissing() {
        IBeaconRequest request = request(
                "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0", 1, 3, 404L, null);
        when(siteClient.requireSite(404L))
                .thenThrow(ApiException.badRequest("Không tìm thấy di tích với ID: 404"));

        assertThatThrownBy(() -> service.createBeacon(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("di tích");
        verify(beaconRepository, never()).save(any());
    }
}
