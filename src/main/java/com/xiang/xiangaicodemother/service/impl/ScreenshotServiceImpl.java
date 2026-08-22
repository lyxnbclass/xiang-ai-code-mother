package com.xiang.xiangaicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import com.xiang.xiangaicodemother.config.properties.ScreenshotProperties;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.service.ScreenshotService;
import com.xiang.xiangaicodemother.storage.ObjectStorageService;
import com.xiang.xiangaicodemother.utils.WebScreenshotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 生成网页截图、上传对象存储并清理本地临时文件。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScreenshotServiceImpl implements ScreenshotService {

    private final WebScreenshotUtils webScreenshotUtils;

    private final ObjectStorageService objectStorageService;

    private final ScreenshotProperties properties;

    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && objectStorageService.isAvailable();
    }

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        if (!isEnabled()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "应用封面截图或对象存储未启用");
        }
        Path localScreenshot = webScreenshotUtils.saveWebPageScreenshot(webUrl);
        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectKey = "screenshots/%s/%s.jpg".formatted(datePath, UUID.randomUUID());
            return objectStorageService.upload(objectKey, localScreenshot.toFile());
        } finally {
            FileUtil.del(localScreenshot.getParent().toFile());
        }
    }
}
