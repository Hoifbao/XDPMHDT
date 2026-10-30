package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.IBeaconRequest;
import com.smartheritage.backend.dto.IBeaconResponse;
import com.smartheritage.backend.entity.Artifact;
import com.smartheritage.backend.entity.HeritageSite;
import com.smartheritage.backend.entity.IBeacon;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.HeritageSiteRepository;
import com.smartheritage.backend.repository.IBeaconRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IBeaconServiceTest {

    private static final String UUID = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0";

    @Mock IBeaconRepository beaconRepository;
    @Mock ArtifactRepository artifactRepository;
    @Mock HeritageSiteRepository siteRepository;
    @InjectMocks IBeaconService service;

    private HeritageSite site(long id) {
        return HeritageSite.builder().id(id).name("Site " + id).build();
    }

    private Artifact artifact(long id, HeritageSite site) {
        return Artifact.builder().id(id).name("Artifact " + id).site(site).build();
    }

    private IBeacon beacon(long id, HeritageSite site, Artifact artifact, String status) {
        return IBeacon.builder().id(id).uuid(UUID).major(1).minor((int) id)
                .site(site).artifact(artifact).status(status).build();
    }

    private IBeaconRequest request(String uuid, Integer major, Integer minor, Long siteId) {
        IBeaconRequest req = new IBeaconRequest();
        req.setUuid(uuid);
        req.setMajor(major);
        req.setMinor(minor);
        req.setSiteId(siteId);
        return req;
    }

    @Test
    void registryReturnsOnlyActiveBeacons() {
        HeritageSite s = site(1);
        IBeacon active = beacon(1, s, artifact(1, s), "ACTIVE");
        when(beaconRepository.findByStatus("ACTIVE")).thenReturn(List.of(active));

        List<IBeaconResponse> result = service.getRegistry();

        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getStatus());
        verify(beaconRepository).findByStatus("ACTIVE");
        verify(beaconRepository, never()).findAll();
    }

    @Test
    void registryIsEmptyWhenNoActiveBeacons() {
        when(beaconRepository.findByStatus("ACTIVE")).thenReturn(List.of());

        assertTrue(service.getRegistry().isEmpty());
    }

    @Test
    void assignArtifactFromSameSiteIsSaved() {
        HeritageSite s = site(1);
        IBeacon beacon = beacon(1, s, null, "ACTIVE");
        Artifact art = artifact(5, s);
        when(beaconRepository.findById(1L)).thenReturn(Optional.of(beacon));
        when(artifactRepository.findById(5L)).thenReturn(Optional.of(art));

        IBeaconResponse res = service.assignArtifact(1L, 5L);

        assertEquals(5L, res.getArtifactId());
        assertEquals(art, beacon.getArtifact());
        verify(beaconRepository).save(beacon);
    }

    @Test
    void assignArtifactFromOtherSiteIsRejected() {
        HeritageSite beaconSite = site(1);
        IBeacon beacon = beacon(1, beaconSite, null, "ACTIVE");
        Artifact art = artifact(9, site(2));
        when(beaconRepository.findById(1L)).thenReturn(Optional.of(beacon));
        when(artifactRepository.findById(9L)).thenReturn(Optional.of(art));

        assertThrows(IllegalArgumentException.class, () -> service.assignArtifact(1L, 9L));
        verify(beaconRepository, never()).save(any());
    }

    @Test
    void assignNullUnassignsArtifact() {
        HeritageSite s = site(1);
        IBeacon beacon = beacon(1, s, artifact(5, s), "ACTIVE");
        when(beaconRepository.findById(1L)).thenReturn(Optional.of(beacon));

        IBeaconResponse res = service.assignArtifact(1L, null);

        assertNull(res.getArtifactId());
        assertNull(beacon.getArtifact());
        verify(beaconRepository).save(beacon);
    }

    @Test
    void assignToMissingBeaconThrowsNotFound() {
        when(beaconRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.assignArtifact(999L, 1L));
    }

    @Test
    void assignMissingArtifactIsRejected() {
        HeritageSite s = site(1);
        IBeacon beacon = beacon(1, s, null, "ACTIVE");
        when(beaconRepository.findById(1L)).thenReturn(Optional.of(beacon));
        when(artifactRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.assignArtifact(1L, 999L));
    }

    @Test
    void createNormalizesUuidAndDefaultsStatus() {
        HeritageSite s = site(1);
        when(siteRepository.findById(1L)).thenReturn(Optional.of(s));
        when(beaconRepository.existsByUuidAndMajorAndMinor(anyString(), any(), any())).thenReturn(false);

        IBeaconResponse res = service.createBeacon(request(UUID.toLowerCase(), 1, 7, 1L));

        assertEquals(UUID, res.getUuid());
        assertEquals("ACTIVE", res.getStatus());
    }

    @Test
    void createRejectsDuplicate() {
        when(beaconRepository.existsByUuidAndMajorAndMinor(anyString(), any(), any())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.createBeacon(request(UUID, 1, 7, 1L)));
    }

    @Test
    void createRejectsInvalidUuid() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createBeacon(request("abc", 1, 7, 1L)));
    }

    @Test
    void createRejectsMajorOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createBeacon(request(UUID, 70000, 7, 1L)));
    }
}
