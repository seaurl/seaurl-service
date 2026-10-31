package com.seaurl.platform.users.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;

public class IpUtils {
    private static Logger log = LoggerFactory.getLogger(IpUtils.class);

    /**
     * 获取ip地址
     * @param request
     * @return
     */
    public static String getIpAddr(ServerHttpRequest request){
        String ipAddress = null;
        try {
            List<String> xForwardedForHeader = request.getHeaders().get("X-Forwarded-For");
            if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
                ipAddress = xForwardedForHeader.get(0);
                if(ipAddress.indexOf(",") != -1) {
                    ipAddress = ipAddress.split(",")[0];
                }
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                List<String> proxyClientIPHeader = request.getHeaders().get("Proxy-Client-IP");
                ipAddress = (proxyClientIPHeader != null && !proxyClientIPHeader.isEmpty()) ? proxyClientIPHeader.get(0) : null;
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                List<String> wlProxyClientIPHeader = request.getHeaders().get("WL-Proxy-Client-IP");
                ipAddress = (wlProxyClientIPHeader != null && !wlProxyClientIPHeader.isEmpty()) ? wlProxyClientIPHeader.get(0) : null;
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                List<String> httpClientIPHeader = request.getHeaders().get("HTTP_CLIENT_IP");
                ipAddress = (httpClientIPHeader != null && !httpClientIPHeader.isEmpty()) ? httpClientIPHeader.get(0) : null;
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddress().getAddress().getHostAddress();
            }
        }catch (Exception e) {
            log.error("IPUtils ERROR ",e);
        }
        return ipAddress;
    }
}
