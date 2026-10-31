package com.seaurl.platform.common.auth;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.common.model.UserSession;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TokenSessionService {
    private final UserSessionService userSessionService;
    private final JwtPayloadDecoder jwtPayloadDecoder;

    public TokenSessionService(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
        this.jwtPayloadDecoder = new JwtPayloadDecoder();
    }

    public Mono<LoginUser> resolveLoginUser(String tokenId) {
        return Mono.fromSupplier(() -> {
            UserSession session = userSessionService.queryValid(tokenId);
            if (session == null || session.getAccessToken() == null) {
                return null;
            }
            return jwtPayloadDecoder.decode(session.getAccessToken());
        });
    }
}
