package com.xiang.xiangaicodemother.config;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import com.xiang.xiangaicodemother.config.properties.CosClientProperties;
import com.xiang.xiangaicodemother.config.properties.ScreenshotProperties;
import com.xiang.xiangaicodemother.storage.DisabledObjectStorageService;
import com.xiang.xiangaicodemother.storage.ObjectStorageService;
import com.xiang.xiangaicodemother.storage.TencentCosObjectStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;
import java.net.URI;

/**
 * 截图异步执行器与对象存储配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties({CosClientProperties.class, ScreenshotProperties.class})
public class ScreenshotConfig {

    @Bean("screenshotTaskExecutor")
    public AsyncTaskExecutor screenshotTaskExecutor(ScreenshotProperties properties) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("app-cover-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(Math.max(1, properties.getMaxConcurrency()));
        executor.setTaskTerminationTimeout(Duration.ofSeconds(30).toMillis());
        return executor;
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "cos.client", name = "enabled", havingValue = "true")
    public COSClient cosClient(CosClientProperties properties) {
        if (StrUtil.hasBlank(properties.getHost(), properties.getSecretId(), properties.getSecretKey(),
                properties.getRegion(), properties.getBucket())) {
            throw new IllegalStateException("启用 COS 后必须完整配置 host、secretId、secretKey、region 和 bucket");
        }
        URI host = URI.create(properties.getHost());
        if (host.getHost() == null || !("http".equalsIgnoreCase(host.getScheme())
                || "https".equalsIgnoreCase(host.getScheme()))) {
            throw new IllegalStateException("COS host 必须是完整的 http 或 https 地址");
        }
        COSCredentials credentials = new BasicCOSCredentials(
                properties.getSecretId(), properties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));
        return new COSClient(credentials, clientConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "cos.client", name = "enabled", havingValue = "true")
    public ObjectStorageService cosObjectStorageService(COSClient cosClient,
                                                         CosClientProperties properties) {
        return new TencentCosObjectStorageService(cosClient, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "cos.client", name = "enabled", havingValue = "false",
            matchIfMissing = true)
    public ObjectStorageService disabledObjectStorageService() {
        return new DisabledObjectStorageService();
    }
}
