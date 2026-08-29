package com.xiang.xiangaicodemother.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "screenshot")
public class ScreenshotProperties {

    private int width = 1600;
    private int height = 900;
    private int pageLoadTimeoutSeconds = 30;
    private int readyTimeoutSeconds = 10;
    private int renderDelayMillis = 2000;
    private boolean noSandbox;
    private String driverPath;
    private String browserPath;
}
