package com.xiang.xiangaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.xiang.xiangaicodemother.mapper.AppMapper;
import com.xiang.xiangaicodemother.service.AppCoverService;
import com.xiang.xiangaicodemother.service.ScreenshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 将耗时截图任务放到受限的虚拟线程执行器中，不阻塞部署响应。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AppCoverServiceImpl implements AppCoverService {

    private final ScreenshotService screenshotService;

    private final AppMapper appMapper;

    @Override
    @Async("screenshotTaskExecutor")
    public void generateAppCoverAsync(Long appId, String appUrl) {
        if (!screenshotService.isEnabled()) {
            log.debug("应用封面截图未启用，跳过生成，appId={}", appId);
            return;
        }
        try {
            String coverUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            if (StrUtil.isBlank(coverUrl)) {
                log.warn("应用封面生成结果为空，appId={}", appId);
                return;
            }
            int updatedRows = appMapper.updateCoverById(appId, coverUrl);
            if (updatedRows != 1) {
                log.warn("应用封面回写失败，appId={}", appId);
            }
        } catch (Exception e) {
            log.error("异步生成应用封面失败，appId={}", appId, e);
        }
    }
}
