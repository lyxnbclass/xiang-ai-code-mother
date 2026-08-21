package com.xiang.xiangaicodemother.service;

/**
 * 网页截图上传服务。
 */
public interface ScreenshotService {

    boolean isEnabled();

    String generateAndUploadScreenshot(String webUrl);
}
