package com.xiang.xiangaicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import com.xiang.xiangaicodemother.manager.CosManager;
import com.xiang.xiangaicodemother.service.ScreenshotService;
import com.xiang.xiangaicodemother.utils.WebScreenshotUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** 生成网页截图、上传 COS 并清理本地临时文件。 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScreenshotServiceImpl implements ScreenshotService {

    private final CosManager cosManager;

    private final WebScreenshotUtils webScreenshotUtils;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        log.info("开始生成网页截图，URL：{}", webUrl);
        Path localScreenshot = webScreenshotUtils.saveWebPageScreenshot(webUrl);
        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectKey = "/screenshots/%s/%s.jpg".formatted(datePath, UUID.randomUUID());
            String cosUrl = cosManager.uploadFile(objectKey, localScreenshot.toFile());
            log.info("截图上传成功，URL：{}", cosUrl);
            return cosUrl;
        } finally {
            FileUtil.del(localScreenshot.getParent().toFile());
        }
    }
}
