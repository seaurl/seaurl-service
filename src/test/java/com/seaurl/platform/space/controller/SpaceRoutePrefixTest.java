package com.seaurl.platform.space.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceRoutePrefixTest {
    @Test
    void urlControllerUsesSpacePrefix() {
        RequestMapping mapping = UrlController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).contains("/space");
    }
}
