package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.service.StorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    @Value("${storage.local.base-path:./data/uploads}")
    private String basePath;

    @Value("${storage.local.base-url:http://localhost:7777/api/uploads}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        try {
            Path baseDir = Paths.get(basePath);
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
            log.info("LocalStorageService initialized, basePath={}, baseUrl={}", basePath, baseUrl);
        } catch (IOException e) {
            log.error("Failed to init local storage base path: {}", basePath, e);
        }
    }

    private String buildUrl(String fullPath) {
        return baseUrl + "/" + fullPath;
    }

    private Path resolveFullPath(String fullPath) {
        return Paths.get(basePath, fullPath).normalize();
    }

    private void ensureParentDir(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    @Override
    public String uploadImgBase64(String base64Img, String fullPath) throws IOException {
        String raw = base64Img.split(",")[1];
        byte[] bytes = Base64.getDecoder().decode(raw);
        Path target = resolveFullPath(fullPath);
        ensureParentDir(target);
        Files.write(target, bytes);
        return buildUrl(fullPath);
    }

    @Override
    public String uploadImgByInputStream(MultipartFile file, String fullPath) throws IOException {
        Path target = resolveFullPath(fullPath);
        ensureParentDir(target);
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return buildUrl(fullPath);
    }

    @Override
    public Mono<String> uploadImgByInputStreamNoBlock(FilePart file, String fullPath) {
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return Mono.fromCallable(() -> {
                        Path target = resolveFullPath(fullPath);
                        ensureParentDir(target);
                        Files.write(target, bytes);
                        return buildUrl(fullPath);
                    }).subscribeOn(Schedulers.boundedElastic());
                });
    }

    @Override
    public Mono<String> uploadFilePart(FilePart file, String fullPath) {
        return Mono.fromCallable(() -> {
                    Path target = resolveFullPath(fullPath);
                    ensureParentDir(target);
                    return target;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(target -> file.transferTo(target).thenReturn(buildUrl(fullPath)));
    }

    @Override
    public String uploadFavicon(InputStream stream, String fullPath) throws IOException {
        Path target = resolveFullPath(fullPath);
        ensureParentDir(target);
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        return buildUrl(fullPath);
    }

    @Override
    public String uploadFile(InputStream stream, String fullPath) throws IOException {
        Path target = resolveFullPath(fullPath);
        ensureParentDir(target);
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        return buildUrl(fullPath);
    }

    @Override
    public String uploadFile(MultipartFile file, String fullPath) throws IOException {
        Path target = resolveFullPath(fullPath);
        ensureParentDir(target);
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return buildUrl(fullPath);
    }

    @Override
    public boolean rename(String oldFullPath, String newFullPath) throws IOException {
        Path source = resolveFullPath(oldFullPath);
        Path target = resolveFullPath(newFullPath);
        ensureParentDir(target);
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    @Override
    public boolean copy(String oldFullPath, String newFullPath) throws IOException {
        Path source = resolveFullPath(oldFullPath);
        Path target = resolveFullPath(newFullPath);
        ensureParentDir(target);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    @Override
    public boolean isExist(String filePath) throws IOException {
        Path target = resolveFullPath(filePath);
        return Files.exists(target);
    }

    @Override
    public String uploadLargeFile(MultipartFile file, String fullPath) throws IOException {
        return uploadFile(file, fullPath);
    }

    @Override
    public boolean deleteFile(String filePath) throws IOException {
        Path target = resolveFullPath(filePath);
        return Files.deleteIfExists(target);
    }

    @Override
    public InputStream downloadFile(String filePath) throws IOException {
        Path target = resolveFullPath(filePath);
        return Files.newInputStream(target);
    }
}
