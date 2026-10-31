package com.seaurl.platform.common.context;

import com.seaurl.platform.common.model.LoginUser;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserContext {
    public static final String USER = "user";
    public static final String AUTH_TOKEN = "auth-token";
    public static final String TOKEN_ID = "token-id";
    public static final String CORRELATION_ID = "correlation-id";

    private String correlationId;
    private String authToken;
    private String tokenId;
    private LoginUser user;
}
