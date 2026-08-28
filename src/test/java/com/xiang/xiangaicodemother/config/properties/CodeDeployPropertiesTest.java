package com.xiang.xiangaicodemother.config.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeDeployPropertiesTest {

    @Test
    void shouldNormalizeTrailingSlashes() {
        CodeDeployProperties properties = new CodeDeployProperties();
        properties.setDeployHost(" https://example.com/dist/// ");

        assertEquals("https://example.com/dist", properties.normalizedDeployHost());
    }

    @Test
    void shouldFallbackWhenDeployHostIsBlank() {
        CodeDeployProperties properties = new CodeDeployProperties();
        properties.setDeployHost("  ");

        assertEquals("http://localhost", properties.normalizedDeployHost());
    }
}
