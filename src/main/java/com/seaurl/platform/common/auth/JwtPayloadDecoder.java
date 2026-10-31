package com.seaurl.platform.common.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seaurl.platform.common.model.LoginUser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtPayloadDecoder {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginUser decode(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            LoginUser user = new LoginUser();
            user.setUid(payload.path("uid").asText(null));
            user.setUserName(payload.path("userName").asText(null));
            user.setEmail(payload.path("email").asText(null));
            user.setAvatar(payload.path("avatar").asText(null));
            user.setMobile(payload.path("mobile").asText(null));
            return user;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to decode JWT payload", ex);
        }
    }
}
