package com.xiang.xiangaicodemother.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯云 COS 客户端配置。
 */
@Data
@ConfigurationProperties(prefix = "cos.client")
public class CosClientProperties {

    private boolean enabled;

    private String host;

    private String secretId;

    private String secretKey;

    private String region;

    private String bucket;
}
