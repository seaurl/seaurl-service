package com.seaurl.platform.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserModelCompatibilityTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsFieldsFromSpaceAndUsersUserDocuments() throws Exception {
        String json = "{" +
                "\"id\":\"1\"," +
                "\"uid\":\"u1\"," +
                "\"userName\":\"alice\"," +
                "\"nickName\":\"Alice\"," +
                "\"backgroundImg\":\"https://cdn/bg.png\"," +
                "\"background\":\"dark\"," +
                "\"color\":\"#111111\"," +
                "\"ipAddr\":\"上海\"" +
                "}";

        User user = objectMapper.readValue(json, User.class);

        assertThat(user.getUid()).isEqualTo("u1");
        assertThat(user.getUserName()).isEqualTo("alice");
        assertThat(user.getBackgroundImg()).isEqualTo("https://cdn/bg.png");
        assertThat(user.getBackground()).isEqualTo("dark");
        assertThat(user.getColor()).isEqualTo("#111111");
        assertThat(user.getIpAddr()).isEqualTo("上海");
    }
}
