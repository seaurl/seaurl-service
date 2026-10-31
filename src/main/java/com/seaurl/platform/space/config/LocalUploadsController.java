package com.seaurl.platform.space.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalUploadsController {

    private static final Logger log = LoggerFactory.getLogger(LocalUploadsController.class);

    @Value("${storage.local.base-path:./data/uploads}")
    private String basePath;

    @GetMapping("/**")
    public Mono<ResponseEntity<Resource>> serveFile(ServerHttpRequest request) {
        String fullPath = request.getPath().pathWithinApplication().value();
        String relativePath = fullPath.substring("/uploads/".length());

        Path basePathAbs = Paths.get(basePath).toAbsolutePath().normalize();
        Path filePath = basePathAbs.resolve(relativePath).normalize();

        if (!filePath.startsWith(basePathAbs) || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            log.warn("File not found: {} (resolved from {})", filePath, relativePath);
            return Mono.just(ResponseEntity.notFound().build());
        }

        MediaType mediaType = guessMediaType(filePath.getFileName().toString());
        Resource resource = new FileSystemResource(filePath);
        return Mono.just(ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource));
    }

    private MediaType guessMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        if (lower.endsWith(".bmp")) return MediaType.valueOf("image/bmp");
        if (lower.endsWith(".webp")) return MediaType.valueOf("image/webp");
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".mp4")) return MediaType.valueOf("video/mp4");
        if (lower.endsWith(".mp3")) return MediaType.valueOf("audio/mpeg");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
