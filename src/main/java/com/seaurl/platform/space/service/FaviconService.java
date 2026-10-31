package com.seaurl.platform.space.service;

/**
 * Favicon 代理服务：解决浏览器跨域（CORP/CSP/防盗链）导致前端 <img> 加载失败的问题。
 * <p>
 * 流程：查 Domain 表拿原站 favicon URL → 服务器请求字节流 → 写 Redis 缓存（TTL 7 天） → 返回。
 */
public interface FaviconService {

    /**
     * 按域名获取 favicon 字节流（带 MIME），命中缓存走 Redis，否则回源。
     * 返回 null 表示该域名无可用 favicon（调用方返回默认图标）。
     */
    byte[] getFaviconBytes(String domain);
}
