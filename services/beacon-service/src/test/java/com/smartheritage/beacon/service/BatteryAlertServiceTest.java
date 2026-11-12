package com.smartheritage.beacon.service;

import com.smartheritage.beacon.entity.IBeacon;
import com.smartheritage.beacon.events.BeaconBatteryLowData;
import com.smartheritage.beacon.repository.IBeaconRepository;
import com.smartheritage.common.events.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cảnh báo pin beacon - Khoa, W6.
 * Repository + EventPublisher mock: kiểm đúng beacon phát sự kiện, chống báo lặp và xóa cờ khi hồi pin.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatteryAlertServiceTest {

    @Mock IBeaconRepository beaconRepository;
    @Mock EventPublisher eventPublisher;

    private BatteryAlertService service;

    @BeforeEach
    void setUp() {
        service = new BatteryAlertService(beaconRepository, eventPublisher);
    }

    private IBeacon beacon(Long id, Integer batteryLevel, String status) {
        return IBeacon.builder()
                .id(id).siteId(1L)
                .uuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0")
                .major(1).minor(id.intValue())
                .batteryLevel(batteryLevel).status(status)
                .build();
    }

    @Test
    void alertsLowActiveBeaconAndMarksFlag() {
        IBeacon low = beacon(4L, 15, "ACTIVE");
        when(beaconRepository.findByStatusAndBatteryLevelLessThan("ACTIVE", 20)).thenReturn(List.of(low));
        when(beaconRepository.findByBatteryLevelGreaterThanEqualAndBatteryAlertedAtNotNull(20))
                .thenReturn(List.of());

        int alerted = service.checkAndAlert();

        assertThat(alerted).isEqualTo(1);
        ArgumentCaptor<BeaconBatteryLowData> data = ArgumentCaptor.forClass(BeaconBatteryLowData.class);
        verify(eventPublisher).publishAfterCommit(
                eq(BatteryAlertService.ROUTING_KEY), eq(BatteryAlertService.EVENT_TYPE), data.capture());
        assertThat(data.getValue().beaconId()).isEqualTo(4L);
        assertThat(data.getValue().siteId()).isEqualTo(1L);
        assertThat(data.getValue().batteryLevel()).isEqualTo(15);
        assertThat(low.getBatteryAlertedAt()).isNotNull(); // chống báo lặp
        verify(beaconRepository).save(low);
    }

    @Test
    void doesNotAlertSameBeaconTwice() {
        IBeacon low = beacon(4L, 15, "ACTIVE");
        low.setBatteryAlertedAt(java.time.LocalDateTime.now()); // đã cảnh báo lượt trước
        when(beaconRepository.findByStatusAndBatteryLevelLessThan("ACTIVE", 20)).thenReturn(List.of(low));
        when(beaconRepository.findByBatteryLevelGreaterThanEqualAndBatteryAlertedAtNotNull(20))
                .thenReturn(List.of());

        int alerted = service.checkAndAlert();

        assertThat(alerted).isZero();
        verify(eventPublisher, never()).publishAfterCommit(any(), any(), any());
    }

    @Test
    void ignoresInactiveBeacon() {
        // Repository đã lọc theo status=ACTIVE; test phòng hờ data không hợp lệ
        when(beaconRepository.findByStatusAndBatteryLevelLessThan("ACTIVE", 20)).thenReturn(List.of());
        when(beaconRepository.findByBatteryLevelGreaterThanEqualAndBatteryAlertedAtNotNull(20))
                .thenReturn(List.of());

        assertThat(service.checkAndAlert()).isZero();
        verify(eventPublisher, never()).publishAfterCommit(any(), any(), any());
    }

    @Test
    void clearsFlagWhenBatteryRecovered() {
        IBeacon recovered = beacon(2L, 85, "ACTIVE");
        recovered.setBatteryAlertedAt(java.time.LocalDateTime.now());
        when(beaconRepository.findByStatusAndBatteryLevelLessThan("ACTIVE", 20)).thenReturn(List.of());
        when(beaconRepository.findByBatteryLevelGreaterThanEqualAndBatteryAlertedAtNotNull(20))
                .thenReturn(List.of(recovered));

        int alerted = service.checkAndAlert();

        assertThat(alerted).isZero();
        assertThat(recovered.getBatteryAlertedAt()).isNull(); // cho phép cảnh báo lần sau
        verify(beaconRepository).save(recovered);
    }
}
