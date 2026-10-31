package com.seaurl.platform.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlModelCompatibilityTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsStringAndBooleanPrivateValues() throws Exception {
        Url stringPrivate = objectMapper.readValue("{\"id\":\"1\",\"isPrivate\":\"0\",\"keywords\":\"java\"}", Url.class);
        Url booleanPrivate = objectMapper.readValue("{\"id\":\"2\",\"isPrivate\":true}", Url.class);

        assertThat(stringPrivate.getIsPrivate()).isEqualTo("0");
        assertThat(booleanPrivate.getIsPrivate()).isEqualTo("true");
        assertThat(stringPrivate.getKeywords()).isEqualTo("java");
    }
}
