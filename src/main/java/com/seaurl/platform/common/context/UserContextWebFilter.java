package com.seaurl.platform.common.context;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-99)
public class UserContextWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userHeader = exchange.getRequest().getHeaders().getFirst(UserContext.USER);
        if (userHeader != null) {
            UserContextHolder.getContext().setAuthToken(userHeader);
        }
        return chain.filter(exchange).doFinally(signalType -> UserContextHolder.clear());
    }
}
