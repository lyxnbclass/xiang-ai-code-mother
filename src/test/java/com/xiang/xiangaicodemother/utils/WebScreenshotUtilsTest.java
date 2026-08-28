package com.xiang.xiangaicodemother.utils;

import cn.hutool.core.io.FileUtil;
import com.sun.net.httpserver.HttpServer;
import com.xiang.xiangaicodemother.config.properties.CodeDeployProperties;
import com.xiang.xiangaicodemother.config.properties.ScreenshotProperties;
import com.xiang.xiangaicodemother.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    @EnabledIfSystemProperty(named = "screenshot.integration", matches = "true")
    void shouldCaptureLocalPageWithHeadlessChrome() throws Exception {
        int port = 18080;
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/demo/", exchange -> {
            byte[] body = "<html><body><h1>Screenshot works</h1></body></html>"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        ScreenshotProperties properties = new ScreenshotProperties();
        properties.setRenderDelayMillis(0);
        CodeDeployProperties integrationDeployProperties = new CodeDeployProperties();
        integrationDeployProperties.setDeployHost("http://localhost:" + port);
        WebScreenshotUtils integrationUtils = new WebScreenshotUtils(properties, integrationDeployProperties);
        Path screenshot = null;
        try {
            screenshot = integrationUtils.saveWebPageScreenshot("http://localhost:" + port + "/demo/");
            org.junit.jupiter.api.Assertions.assertTrue(Files.size(screenshot) > 0);
        } finally {
            server.stop(0);
            if (screenshot != null) {
                FileUtil.del(screenshot.getParent().toFile());
            }
        }
    }
}
