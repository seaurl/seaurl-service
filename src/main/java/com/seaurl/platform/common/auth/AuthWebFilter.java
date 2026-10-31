package com.seaurl.platform.common.auth;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;
import com.seaurl.platform.common.model.LoginUser;
import com.alibaba.fastjson.JSON;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class AuthWebFilter implements WebFilter, Ordered {
    private final TokenSessionService tokenSessionService;
    private final Policy policy = new Policy();

    public AuthWebFilter(TokenSessionService tokenSessionService) {
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // OPTIONS 预检请求直接放行，避免 CORS 问题
        if (exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().pathWithinApplication().value();

        if (!policy.requiresAuthentication(path)) {
            return chain.filter(exchange);
        }

        String tokenId = exchange.getRequest().getHeaders().getFirst(UserContext.TOKEN_ID);
        if (!StringUtils.hasLength(tokenId)) {
            return writeAuthError(exchange);
        }

        return tokenSessionService.resolveLoginUser(tokenId)
                .flatMap(loginUser -> {
                    UserContextHolder.getContext().setTokenId(tokenId);
                    UserContextHolder.getContext().setUser(loginUser);
                    return chain.filter(exchange);
                })
                .switchIfEmpty(writeAuthError(exchange));
    }

    private Mono<Void> writeAuthError(ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        String body = JSON.toJSONString(new ResultInfo<>(ResultStatus.AUTH_ERROR));
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Policy {
        public boolean requiresAuthentication(String path) {
            return !(path.startsWith("/space/info/")
                    || path.equals("/space/info")
                    || path.startsWith("/space/web/")
                    || path.equals("/space/web")
                    || path.startsWith("/users/info/")
                    || path.equals("/users/info")
                    || path.startsWith("/users/web/")
                    || path.equals("/users/web")
                    || path.startsWith("/uploads")
                    || path.startsWith("/websocket"));
        }
    }
}
