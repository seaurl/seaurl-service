package com.seaurl.platform.space.service;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface StorageService {
    String uploadImgBase64(String base64Img, String fullPath) throws IOException;

    String uploadImgByInputStream(MultipartFile file, String fullPath) throws IOException;

    Mono<String> uploadImgByInputStreamNoBlock(FilePart file, String fullPath);

    /**
     * 默认实现会把整个文件聚合进内存再上传（适合远端存储）；
     * 本地存储实现必须覆写为流式写入，否则大文件会撑爆堆内存。
     */
    default Mono<String> uploadFilePart(FilePart file, String fullPath) {
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return Mono.fromCallable(() -> uploadFile(new ByteArrayInputStream(bytes), fullPath))
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    String uploadFavicon(InputStream stream, String fullPath) throws IOException;

    String uploadFile(InputStream stream, String fullPath) throws IOException;

    String uploadFile(MultipartFile file, String fullPath) throws IOException;

    boolean rename(String oldFullPath, String newFullPath) throws IOException;

    boolean copy(String oldFullPath, String newFullPath) throws IOException;

    boolean isExist(String filePath) throws IOException;

    String uploadLargeFile(MultipartFile file, String fullPath) throws IOException;

    boolean deleteFile(String filePath) throws IOException;

    InputStream downloadFile(String filePath) throws IOException;
}
