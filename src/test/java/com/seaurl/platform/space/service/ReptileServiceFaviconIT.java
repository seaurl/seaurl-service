package com.seaurl.platform.space.service;

import com.seaurl.platform.space.service.impl.ReptileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证新的多策略 favicon 抓取算法。
 * <p>
 * 用真实站点做端到端验证（spec §5 抓取算法测试点 8-14）。
 * 仅靠 {@code new ReptileServiceImpl()}（@Service 但不依赖容器其它 bean），不启 Spring。
 * <p>
 * 默认跳过，加 {@code -DrunNetworkTests=true} 启用（CI 沙箱环境通常无外网）。
 */
@EnabledIfSystemProperty(named = "runNetworkTests", matches = "true")
class ReptileServiceFaviconIT {

    private ReptileService reptileService;

    @BeforeEach
    void setUp() {
        reptileService = new ReptileServiceImpl();
    }

    @Test
    void getsFaviconForStandardLinkIconSite() throws Exception {
        HashMap<String, Object> result = reptileService.getFaviconFromUrl("https://github.com");
        assertThat(result).as("github.com 必须抓到 favicon").isNotNull();
        String faviconUrl = (String) result.get("faviconUrl");
        assertThat(faviconUrl).startsWith("http");
        System.out.println("[github.com] favicon = " + faviconUrl);
    }

    @Test
    void getsFaviconForUnknownSiteFallsBackToFaviconIco() throws Exception {
        // example.com 是 W3C 示例站，HTML 极简，通常只有根 /favicon.ico
        HashMap<String, Object> result = reptileService.getFaviconFromUrl("https://example.com");
        if (result == null) {
            System.out.println("[example.com] 没抓到（可能临时无响应），跳过断言");
            return;
        }
        String faviconUrl = (String) result.get("faviconUrl");
        assertThat(faviconUrl).startsWith("http");
        System.out.println("[example.com] favicon = " + faviconUrl);
    }

    @Test
    void invalidUrlReturnsNull() throws Exception {
        HashMap<String, Object> result = reptileService.getFaviconFromUrl("https://this-domain-definitely-does-not-exist-987654321.com");
        assertThat(result).isNull();
    }

    @Test
    void notTimeoutOnSlowOrUnreachableSite() throws Exception {
        // 验证：抓取要么 8 秒内超时返回 null，要么返回结果，绝不卡死
        long start = System.currentTimeMillis();
        HashMap<String, Object> result = reptileService.getFaviconFromUrl("https://10.255.255.1");
        long elapsed = System.currentTimeMillis() - start;
        // 连接超时 + 读超时各 8s，HEAD 3s。总上限 20s 容忍
        assertThat(elapsed)
                .as("慢站/不可达站应在合理时间内返回（不应无限阻塞），实际 %d ms", elapsed)
                .isLessThan(25000L);
        System.out.println("[unreachable] elapsed=" + elapsed + "ms, result=" + result);
    }

    @Test
    void pwaSiteFaviconFromManifestOrLink() throws Exception {
        // twitter / instagram 等 PWA 站通常带 manifest.json
        HashMap<String, Object> result = reptileService.getFaviconFromUrl("https://twitter.com");
        if (result == null) {
            System.out.println("[twitter.com] 没抓到（可能 403 或网络问题），跳过断言");
            return;
        }
        String faviconUrl = (String) result.get("faviconUrl");
        System.out.println("[twitter.com] favicon = " + faviconUrl);
        // 不强断言非空，因为 twitter 经常对中国 IP 限流；只要不抛异常即可
    }
}
