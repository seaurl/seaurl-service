package com.seaurl.platform.users.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class UsersRoutePrefixTest {
    @Test
    void accountControllerUsesUsersPrefix() {
        RequestMapping mapping = AccountController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).contains("/users");
    }
}
