package com.xiang.xiangaicodemother.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用封面截图配置。
 */
@Data
@ConfigurationProperties(prefix = "screenshot")
public class ScreenshotProperties {

    private boolean enabled = true;

    private int width = 1600;

    private int height = 900;

    private int pageLoadTimeoutSeconds = 30;

    private int readyTimeoutSeconds = 10;

    private int renderDelayMillis = 2000;

    private int maxConcurrency = 2;

    private boolean noSandbox;

    private String driverPath;

    private String browserPath;
}
