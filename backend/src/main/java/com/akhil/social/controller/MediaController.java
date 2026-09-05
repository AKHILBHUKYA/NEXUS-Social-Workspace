package com.akhil.social.controller;

import com.akhil.social.entity.User;
import com.akhil.social.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm", "video/quicktime"
    );

    @Value("${app.media.max-size-mb:10}")
    private int maxSizeMb;

    @Value("${app.media.storage-path:./uploads}")
    private String storagePath;

    @Value("${app.media.public-base-url:}")
    private String publicBaseUrl;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @AuthenticationPrincipal User user) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApiException("No file provided", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new ApiException("Unsupported file type. Allowed: images and videos", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        long maxBytes = maxSizeMb * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new ApiException("File too large. Max " + maxSizeMb + "MB", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Path dir = Paths.get(storagePath);
        Files.createDirectories(dir);
        String ext = extension(file.getOriginalFilename(), contentType);
        String name = UUID.randomUUID() + ext;
        Path dest = dir.resolve(name);
        file.transferTo(dest.toFile());

        String mediaType = contentType.startsWith("video/") ? "VIDEO" : "IMAGE";
        String url;
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            url = publicBaseUrl.replaceAll("/$", "") + "/" + name;
        } else {
            // Local/demo: serve via backend static mapping
            url = "/api/media/files/" + name;
        }

        return Map.of(
                "success", true,
                "mediaUrl", url,
                "mediaType", mediaType,
                "filename", name,
                "size", file.getSize()
        );
    }

    @GetMapping("/files/{filename}")
    public org.springframework.http.ResponseEntity<byte[]> serve(@PathVariable String filename) throws IOException {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ApiException("Invalid filename", HttpStatus.BAD_REQUEST);
        }
        Path path = Paths.get(storagePath).resolve(filename);
        if (!Files.exists(path)) {
            throw new ApiException("File not found", HttpStatus.NOT_FOUND);
        }
        String probe = Files.probeContentType(path);
        MediaType mt = probe != null ? MediaType.parseMediaType(probe) : MediaType.APPLICATION_OCTET_STREAM;
        return org.springframework.http.ResponseEntity.ok()
                .contentType(mt)
                .body(Files.readAllBytes(path));
    }

    private String extension(String original, String contentType) {
        if (original != null && original.contains(".")) {
            String e = original.substring(original.lastIndexOf('.')).toLowerCase();
            if (e.matches("\\.(jpe?g|png|gif|webp|mp4|webm|mov)")) return e;
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "video/quicktime" -> ".mov";
            default -> ".bin";
        };
    }
}
