package com.smartheritage.content;

import com.smartheritage.content.service.MediaStorageService;
import com.smartheritage.common.web.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class MediaStorageServiceTest {
    @TempDir Path directory;

    @Test
    void enforcesLimitsBeforeReadingOrSavingAndBlocksTraversal() throws Exception {
        var storage = new MediaStorageService(directory.toString());
        for (String extension : new String[]{"png", "mp3", "mp4"}) {
            var file = new MockMultipartFile("file", "large." + extension, "application/octet-stream", new byte[]{1}) {
                @Override public long getSize() { return 101L * 1024 * 1024; }
            };
            assertThrows(ApiException.class, () -> storage.store(file));
        }
        assertThrows(ApiException.class, () -> storage.load("../secret.png"));
        try (var files = java.nio.file.Files.list(directory)) { assertEquals(0, files.count()); }
    }
}
