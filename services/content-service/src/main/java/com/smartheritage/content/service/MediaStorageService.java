package com.smartheritage.content.service;

import com.smartheritage.common.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class MediaStorageService {
    private final Path root;
    public record Stored(String name, String mediaType, String contentType, long size) {}
    private record Format(String type, String mime, int limitMb) {}
    private static final Map<String, Format> FORMATS = Map.of(
            "jpg", new Format("IMAGE", "image/jpeg", 10), "jpeg", new Format("IMAGE", "image/jpeg", 10),
            "png", new Format("IMAGE", "image/png", 10), "webp", new Format("IMAGE", "image/webp", 10),
            "mp3", new Format("AUDIO", "audio/mpeg", 30), "wav", new Format("AUDIO", "audio/wav", 30),
            "ogg", new Format("AUDIO", "audio/ogg", 30), "m4a", new Format("AUDIO", "audio/mp4", 30),
            "mp4", new Format("VIDEO", "video/mp4", 100), "webm", new Format("VIDEO", "video/webm", 100));

    public MediaStorageService(@Value("${app.media.directory:./uploads}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    public Stored store(MultipartFile file) throws IOException {
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        Format format = FORMATS.get(ext);
        if (format == null || file.isEmpty()) throw ApiException.badRequest("File rỗng hoặc định dạng không hỗ trợ");
        if (file.getSize() > format.limitMb * 1024L * 1024)
            throw ApiException.badRequest("File " + format.type + " tối đa " + format.limitMb + " MB");
        byte[] header;
        try (InputStream input = file.getInputStream()) { header = input.readNBytes(16); }
        if (!matches(ext, header)) throw ApiException.badRequest("Nội dung file không khớp định dạng");
        Files.createDirectories(root);
        String name = UUID.randomUUID() + "." + ext;
        Path target = root.resolve(name);
        try (InputStream input = file.getInputStream()) { Files.copy(input, target); }
        catch (IOException e) { Files.deleteIfExists(target); throw e; }
        return new Stored(name, format.type, format.mime, file.getSize());
    }

    public Resource load(String name) throws IOException {
        if (!name.matches("[0-9a-f-]{36}\\.(jpg|jpeg|png|webp|mp3|wav|ogg|m4a|mp4|webm)"))
            throw ApiException.notFound("Không tìm thấy file");
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) throw ApiException.notFound("Không tìm thấy file");
        return new UrlResource(path.toUri());
    }

    public String contentType(String name) { return FORMATS.get(name.substring(name.lastIndexOf('.') + 1)).mime; }

    private boolean matches(String ext, byte[] h) {
        if (h.length < 12) return false;
        return switch (ext) {
            case "jpg", "jpeg" -> (h[0] & 255) == 255 && (h[1] & 255) == 216 && (h[2] & 255) == 255;
            case "png" -> Arrays.equals(Arrays.copyOf(h, 8), new byte[]{(byte)137,80,78,71,13,10,26,10});
            case "webp" -> ascii(h, 0, "RIFF") && ascii(h, 8, "WEBP");
            case "wav" -> ascii(h, 0, "RIFF") && ascii(h, 8, "WAVE");
            case "ogg" -> ascii(h, 0, "OggS");
            case "mp3" -> ascii(h, 0, "ID3") || ((h[0] & 255) == 255 && (h[1] & 224) == 224);
            case "m4a", "mp4" -> ascii(h, 4, "ftyp");
            case "webm" -> (h[0] & 255) == 26 && (h[1] & 255) == 69 && (h[2] & 255) == 223 && (h[3] & 255) == 163;
            default -> false;
        };
    }

    private boolean ascii(byte[] h, int offset, String text) {
        for (int i = 0; i < text.length(); i++) if (h[offset + i] != text.charAt(i)) return false;
        return true;
    }
}
