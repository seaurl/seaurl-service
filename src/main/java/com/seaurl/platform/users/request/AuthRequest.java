package com.seaurl.platform.users.request;

import lombok.Data;

@Data
public class AuthRequest {
    private String userName;
    private String password;
    private String refreshToken;
}
