package com.xiang.xiangaicodemother.utils;

import com.xiang.xiangaicodemother.config.properties.CodeDeployProperties;
import com.xiang.xiangaicodemother.config.properties.ScreenshotProperties;
import com.xiang.xiangaicodemother.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebScreenshotUtilsTest {

    private final CodeDeployProperties codeDeployProperties = new CodeDeployProperties();
    private final WebScreenshotUtils utils = new WebScreenshotUtils(
            new ScreenshotProperties(), codeDeployProperties);

    @Test
    void shouldOnlyAllowConfiguredDeployOrigin() {
        assertDoesNotThrow(() -> utils.validateScreenshotUrl(
                codeDeployProperties.normalizedDeployHost() + "/app-key/"));
        assertThrows(BusinessException.class,
                () -> utils.validateScreenshotUrl("http://localhost:6553/api/private"));
        assertThrows(BusinessException.class,
                () -> utils.validateScreenshotUrl("https://example.com/"));
        assertThrows(BusinessException.class,
                () -> utils.validateScreenshotUrl("http://user@localhost/app-key/"));
    }
}
