package com.seaurl.platform.space.service.client;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;

@Service
public class HttpDownloadWebClient {

    @Resource
    private WebClient.Builder webClientBuilder;

    @Resource
    private WebClient.Builder defaultWebClientBuilder;

    public Mono<byte[]> downloadAsBytes(String url) {
        WebClient webClient = defaultWebClientBuilder.build();
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class);
    }

    public Mono<InputStream> downloadAsStream(String url) {
        return downloadAsBytes(url)
                .map(ByteArrayInputStream::new);
    }
}
