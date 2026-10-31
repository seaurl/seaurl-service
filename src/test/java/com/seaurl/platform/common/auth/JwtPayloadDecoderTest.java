package com.seaurl.platform.common.auth;

import com.seaurl.platform.common.model.LoginUser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPayloadDecoderTest {
    @Test
    void decodesLoginUserFromJwtPayloadWithoutVerifyingSignature() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{" +
                "\"uid\":\"u1\"," +
                "\"userName\":\"alice\"," +
                "\"email\":\"alice@example.com\"," +
                "\"avatar\":\"https://cdn/avatar.png\"," +
                "\"mobile\":\"13800000000\"" +
                "}").getBytes(StandardCharsets.UTF_8));
        String jwt = header + "." + payload + ".signature";

        LoginUser user = new JwtPayloadDecoder().decode(jwt);

        assertThat(user.getUid()).isEqualTo("u1");
        assertThat(user.getUserName()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getAvatar()).isEqualTo("https://cdn/avatar.png");
        assertThat(user.getMobile()).isEqualTo("13800000000");
    }
}
