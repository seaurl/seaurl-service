package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.service.ReptileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

@Service
public class ReptileServiceImpl implements ReptileService {
    private static Logger log = LoggerFactory.getLogger(ReptileServiceImpl.class);

    // 模拟 Chrome，避免目标站对 Java 默认 UA 返回 403
    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Jsoup 连接 + 读取超时（毫秒），防止慢站卡死 addUrl
    private static final int JSOUP_TIMEOUT_MS = 8000;

    // HEAD 验证 / favicon.ico fallback 时的超时
    private static final int HEAD_TIMEOUT_MS = 3000;

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 多策略 fallback 抓取 favicon：
     *   1. HTML link 标签按优先级解析（SVG > 大尺寸 PNG > apple-touch-icon > mask-icon > fluid-icon > shortcut icon > 任意 icon）
     *   2. link rel=manifest → 拉 manifest.json → 取最大尺寸 icon
     *   3. Fallback {origin}/favicon.ico（HEAD 验证可达）
     *   4. 全部失败返回 null
     *
     * @return 含 faviconUrl、suffix 的 HashMap；suffix 仅做兼容保留（新方案不需要）
     */
    public HashMap<String, Object> getFaviconFromUrl(String url) {
        try {
            // 规范化：从入参 URL 提取 origin（protocol://host），用于后续拼相对路径
            URI origin = URI.create(url);
            String baseUrl = origin.getScheme() + "://" + origin.getHost();

            // 只连一次首页（带 UA + 超时）
            Document doc = Jsoup.connect(url)
                    .userAgent(CHROME_UA)
                    .timeout(JSOUP_TIMEOUT_MS)
                    .get();

            // 策略 1：HTML link 标签按优先级解析
            String linkIcon = extractBestLinkIcon(doc, baseUrl);
            if (StringUtils.hasLength(linkIcon)) {
                return buildResult(linkIcon);
            }

            // 策略 2：解析 PWA manifest.json
            String manifestIcon = extractFromManifest(doc, baseUrl);
            if (StringUtils.hasLength(manifestIcon)) {
                return buildResult(manifestIcon);
            }

            // 策略 3：Fallback /favicon.ico（W3C 默认路径，HEAD 验证）
            String defaultIcon = baseUrl + "/favicon.ico";
            if (isReachable(defaultIcon)) {
                return buildResult(defaultIcon);
            }

        } catch (Exception ex) {
            log.warn("getFavicon err={}", ex.getMessage());
        }
        return null;
    }

    /**
     * 构建返回结构，保持与原方法签名兼容（含 faviconUrl + suffix）。
     * data: URI 不计算 suffix（返回 null）。
     */
    private static HashMap<String, Object> buildResult(String faviconUrl) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("faviconUrl", faviconUrl);
        if (!faviconUrl.startsWith("data:")) {
            result.put("suffix", FilenameUtils.getExtension(faviconUrl));
        } else {
            result.put("suffix", null);
        }
        return result;
    }

    /**
     * 按优先级解析 HTML link 标签。
     * 顺序：SVG icon → 任意 icon（按 sizes 选最大）→ apple-touch-icon → mask-icon → fluid-icon → shortcut icon
     */
    private String extractBestLinkIcon(Document doc, String baseUrl) {
        // 1) 优先：SVG 矢量（type=image/svg+xml）
        String svg = pickFirstBySelector(doc, baseUrl, "link[rel~=icon][type=image/svg\\+xml]", null);
        if (StringUtils.hasLength(svg)) return svg;

        // 2) icon 任意 type，按 sizes 取最大（空 sizes 当 0 处理，仍可命中）
        String bestIcon = pickLargestBySelector(doc, baseUrl, "link[rel~=icon]");
        if (StringUtils.hasLength(bestIcon)) return bestIcon;

        // 3) apple-touch-icon（通常 180x180 PNG）
        String apple = pickLargestBySelector(doc, baseUrl, "link[rel~=apple-touch-icon]");
        if (StringUtils.hasLength(apple)) return apple;

        // 4) mask-icon / fluid-icon 变体
        String mask = pickFirstBySelector(doc, baseUrl, "link[rel~=mask-icon]", null);
        if (StringUtils.hasLength(mask)) return mask;
        String fluid = pickFirstBySelector(doc, baseUrl, "link[rel~=fluid-icon]", null);
        if (StringUtils.hasLength(fluid)) return fluid;

        // 5) shortcut icon（老式声明）
        String shortcut = pickFirstBySelector(doc, baseUrl, "link[rel~=shortcut\\sicon]", null);
        if (StringUtils.hasLength(shortcut)) return shortcut;

        return null;
    }

    /**
     * 按 CSS 选择器取第一个有效 href。
     * typeFilter 非空时同时过滤 type 属性。
     */
    private String pickFirstBySelector(Document doc, String baseUrl, String selector, String typeFilter) {
        Elements els = doc.select(selector);
        for (Element el : els) {
            if (typeFilter != null && !typeFilter.equals(el.attr("type"))) continue;
            String href = resolveHref(el, baseUrl);
            if (StringUtils.hasLength(href)) return href;
        }
        return null;
    }

    /**
     * 按 CSS 选择器取 sizes 最大的 link。
     * 解析 sizes="192x192" 提取数值，无 sizes 当 0；多个同尺寸取第一个。
     */
    private String pickLargestBySelector(Document doc, String baseUrl, String selector) {
        Elements els = doc.select(selector);
        String best = null;
        int bestSize = -1;
        for (Element el : els) {
            String href = resolveHref(el, baseUrl);
            if (!StringUtils.hasLength(href)) continue;
            int size = parseSize(el.attr("sizes"));
            if (size > bestSize) {
                bestSize = size;
                best = href;
            }
        }
        return best;
    }

    /**
     * 解析 link 元素的 href：优先 absUrl（jsoup 已解析绝对路径），否则手动 resolve 相对路径。
     * 仅接受 data:image/... 内联图标（其它 data:, / data:text/ 等空占位视为无效）。
     */
    private String resolveHref(Element el, String baseUrl) {
        String abs = el.absUrl("href");
        if (StringUtils.hasLength(abs)) {
            // jsoup 对 data: href 也会返回原字符串（如 "data:,"），需再次校验
            if (abs.startsWith("data:") && !abs.startsWith("data:image/")) return null;
            return abs;
        }
        String raw = el.attr("href");
        if (!StringUtils.hasLength(raw)) return null;
        if (raw.startsWith("data:image/")) return raw;
        // data:, 等空占位
        if (raw.startsWith("data:")) return null;
        return resolveUrl(baseUrl, raw);
    }

    /**
     * 解析 PWA manifest.json，从 icons 数组取 sizes 最大的 src。
     */
    private String extractFromManifest(Document doc, String baseUrl) {
        Element manifestLink = doc.selectFirst("link[rel=manifest]");
        if (manifestLink == null) return null;
        String manifestHref = resolveHref(manifestLink, baseUrl);
        if (!StringUtils.hasLength(manifestHref)) return null;

        try {
            String json = httpGet(manifestHref, "application/json");
            if (json == null) return null;
            JsonNode root = JSON.readTree(json);
            JsonNode icons = root.get("icons");
            if (icons == null || !icons.isArray() || icons.isEmpty()) return null;

            String bestSrc = null;
            int bestSize = -1;
            for (JsonNode icon : icons) {
                String src = icon.hasNonNull("src") ? icon.get("src").asText() : null;
                if (!StringUtils.hasLength(src)) continue;
                // manifest 内 src 可能是相对路径，按 manifest URL 解析
                if (!src.startsWith("http") && !src.startsWith("data:")) {
                    src = resolveUrl(manifestHref, src);
                }
                int size = parseSize(icon.hasNonNull("sizes") ? icon.get("sizes").asText() : "");
                if (size > bestSize) {
                    bestSize = size;
                    bestSrc = src;
                }
            }
            return bestSrc;
        } catch (Exception e) {
            log.warn("manifest parse err={} url={}", e.getMessage(), manifestHref);
            return null;
        }
    }

    /**
     * HEAD 请求验证 URL 可达（200）。
     */
    private boolean isReachable(String faviconUrl) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(faviconUrl).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(HEAD_TIMEOUT_MS);
            conn.setReadTimeout(HEAD_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", CHROME_UA);
            int code = conn.getResponseCode();
            return code == 200;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 简单 GET 文本（带 UA + 超时 + accept）。失败返回 null。
     */
    private String httpGet(String url, String accept) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(HEAD_TIMEOUT_MS);
            conn.setReadTimeout(HEAD_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", CHROME_UA);
            if (accept != null) conn.setRequestProperty("Accept", accept);
            if (conn.getResponseCode() != 200) return null;
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString(java.nio.charset.StandardCharsets.UTF_8.name());
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 规范化相对路径：baseUrl 必须以 / 结尾或不带尾斜杠都能正确解析。
     * 支持 "//host/path"（协议相对）、"/path"（根相对）、"path"（相对）。
     */
    private static String resolveUrl(String baseUrl, String rel) {
        try {
            // 协议相对 //host/path
            if (rel.startsWith("//")) {
                return URI.create(baseUrl).getScheme() + ":" + rel;
            }
            return new URL(new URL(baseUrl), rel).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析 sizes 属性（"192x192" / "192x192 256x256" / "any"）取最大数值。
     * "any" 视为最大值（矢量/自适应）。无法解析返回 0。
     */
    private static int parseSize(String sizes) {
        if (!StringUtils.hasLength(sizes)) return 0;
        if (sizes.toLowerCase().contains("any")) return Integer.MAX_VALUE;
        Matcher m = Pattern.compile("(\\d+)x\\d+", Pattern.CASE_INSENSITIVE).matcher(sizes);
        int max = 0;
        while (m.find()) {
            try {
                int v = Integer.parseInt(m.group(1));
                if (v > max) max = v;
            } catch (NumberFormatException ignored) {
            }
        }
        return max;
    }

    /**
     * 获取url中的域名
     *
     * @param urlStr
     * @return
     */
    @Override
    public String getDomain(String urlStr) throws IOException {
        URL url = new URL(urlStr);//_url里面放url链接（String）
        return url.getHost();
    }

    /**
     * 获取网址中包含的title
     *
     * @param url
     * @return
     * @throws IOException
     */
    @Override
    public String getUrlTitle(String url) throws IOException {
        String tilte = "";
        //还是一样先从一个URL加载一个Document对象。
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("head");
        Elements urlTitle = links.get(0).select("title");
        tilte = urlTitle.get(0).text();
        log.info("tilte={}", tilte);
        return tilte;
    }

    @Override
    public HashMap getUrlTitleAndDesc(String url) throws IOException {
        String tilte = "";
        String description = "";
        String keywords1 = "";
        String keywords2 = "";
        HashMap<String, String> hashMap = new HashMap<>();
        //还是一样先从一个URL加载一个Document对象。
        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("head");
        Elements urlTitle = links.get(0).select("title");
        tilte = urlTitle.get(0).text();
        log.info("--------------------------------------- tilte: {}", tilte);

        Elements metaTags = doc.select("meta[name=description]");

        Elements keywordsEle1 = doc.select("meta[name=keywords]");
        Elements keywordsEle2 = doc.select("meta[name=Keywords]");
        // 遍历获取到的meta标签
        for (Element metaTag : metaTags) {
            description = metaTag.attr("content");

            log.info("--------------------------------------- description: {}", description);
        }
        for (Element keyword : keywordsEle1) {
            keywords1 = keyword.attr("content");
            log.info("--------------------------------------- keywords1: {}", keywords1);
        }
        for (Element keyword : keywordsEle2) {
            keywords2 = keyword.attr("content");
            log.info("--------------------------------------- keywords2: {}", keywords2);
        }
        if (StringUtils.hasLength(tilte)) {
            hashMap.put("title", tilte);
        }
        if (StringUtils.hasLength(description)) {
            hashMap.put("description", description);
        }
        if (StringUtils.hasLength(keywords1)) {
            hashMap.put("keywords", keywords1);
        }
        if (StringUtils.hasLength(keywords2)) {
            hashMap.put("keywords", keywords2);
        }
        return hashMap;
    }
}
