package com.seaurl.platform.common.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPathPolicyTest {
    @Test
    void publicPathsDoNotRequireAuthentication() {
        AuthWebFilter.Policy policy = new AuthWebFilter.Policy();

        assertThat(policy.requiresAuthentication("/space/info/getCommonUrls")).isFalse();
        assertThat(policy.requiresAuthentication("/users/info/alice")).isFalse();
        assertThat(policy.requiresAuthentication("/users/web/login")).isFalse();
        assertThat(policy.requiresAuthentication("/users/web/register")).isFalse();
        assertThat(policy.requiresAuthentication("/uploads/space/file/xxx/image.png")).isFalse();
        assertThat(policy.requiresAuthentication("/websocket/chat")).isFalse();
    }

    @Test
    void crudPathsRequireAuthentication() {
        AuthWebFilter.Policy policy = new AuthWebFilter.Policy();

        assertThat(policy.requiresAuthentication("/space/crud/file/add")).isTrue();
        assertThat(policy.requiresAuthentication("/users/crud/me")).isTrue();
    }
}
