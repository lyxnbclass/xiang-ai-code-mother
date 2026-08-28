package com.xiang.xiangaicodemother.config.properties;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 生成应用的部署地址配置。
 */
@Component
@ConfigurationProperties(prefix = "code")
@Data
public class CodeDeployProperties {

    private String deployHost = "http://localhost";

    /**
     * 返回不带末尾斜杠的部署地址，避免拼接应用标识时产生重复斜杠。
     */
    public String normalizedDeployHost() {
        String host = StrUtil.blankToDefault(deployHost, "http://localhost").trim();
        return host.replaceAll("/+$", "");
    }
}
