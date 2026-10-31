package com.seaurl.platform.space.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seaurl.platform.space.model.Domain;
import com.seaurl.platform.space.service.DomainService;
import com.seaurl.platform.space.service.FaviconService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Service
public class FaviconServiceImpl implements FaviconService {
    private static final Logger log = LoggerFactory.getLogger(FaviconServiceImpl.class);

    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final int HTTP_TIMEOUT_MS = 5000;

    // 单个 favicon 字节上限（10MB），防止恶意站返回超大 body 撑爆内存
    private static final int MAX_BYTES = 10 * 1024 * 1024;

    @Autowired
    DomainService domainService;

    // 进程内缓存：TTL 7 天，最多 1 万个域名（约 30-50MB 内存）
    // 单实例自托管场景足够；重启后首次回源重抓一次可接受
    private final Cache<String, byte[]> faviconCache = Caffeine.newBuilder()
            .expireAfterWrite(7, TimeUnit.DAYS)
            .maximumSize(10_000)
            .build();

    @Override
    public byte[] getFaviconBytes(String domain) {
        if (!StringUtils.hasLength(domain)) return null;

        byte[] cached = faviconCache.getIfPresent(domain);
        if (cached != null) return cached;

        Domain d = domainService.queryDomainByName(domain);
        if (d == null || !StringUtils.hasLength(d.getFavicon())) {
            return null;
        }
        String faviconUrl = d.getFavicon();

        // data: URI 不走代理（前端浏览器直接渲染即可），代理请求只处理 http/https
        if (faviconUrl.startsWith("data:")) {
            return null;
        }

        byte[] bytes = download(faviconUrl);
        if (bytes == null || bytes.length == 0) return null;

        faviconCache.put(domain, bytes);
        return bytes;
    }

    private byte[] download(String urlStr) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(HTTP_TIMEOUT_MS);
            conn.setReadTimeout(HTTP_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", CHROME_UA);
            if (conn.getResponseCode() != 200) return null;

            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) {
                        log.warn("favicon too large, abort: {} > {}", urlStr, MAX_BYTES);
                        return null;
                    }
                }
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.warn("favicon download err={} url={}", e.getMessage(), urlStr);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
