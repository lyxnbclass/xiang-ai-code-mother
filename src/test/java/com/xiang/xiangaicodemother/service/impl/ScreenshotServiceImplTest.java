package com.xiang.xiangaicodemother.service.impl;

import com.xiang.xiangaicodemother.config.properties.ScreenshotProperties;
import com.xiang.xiangaicodemother.storage.ObjectStorageService;
import com.xiang.xiangaicodemother.utils.WebScreenshotUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreenshotServiceImplTest {

    @Mock
    private WebScreenshotUtils webScreenshotUtils;

    @Mock
    private ObjectStorageService objectStorageService;

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadAndCleanLocalScreenshot() throws Exception {
        Path screenshotDir = Files.createDirectory(tempDir.resolve("screenshot"));
        Path screenshot = Files.writeString(screenshotDir.resolve("cover.jpg"), "image");
        ScreenshotProperties properties = new ScreenshotProperties();
        when(objectStorageService.isAvailable()).thenReturn(true);
        when(webScreenshotUtils.saveWebPageScreenshot("http://localhost/demo/"))
                .thenReturn(screenshot);
        when(objectStorageService.upload(startsWith("screenshots/"), any()))
                .thenReturn("https://cdn.example.com/cover.jpg");
        ScreenshotServiceImpl service = new ScreenshotServiceImpl(
                webScreenshotUtils, objectStorageService, properties);

        String result = service.generateAndUploadScreenshot("http://localhost/demo/");

        assertEquals("https://cdn.example.com/cover.jpg", result);
        assertFalse(Files.exists(screenshotDir));
        verify(objectStorageService).upload(startsWith("screenshots/"), any());
    }
}
