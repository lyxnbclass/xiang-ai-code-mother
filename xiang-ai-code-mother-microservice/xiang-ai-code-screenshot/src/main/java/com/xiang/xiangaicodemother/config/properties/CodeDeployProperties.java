package com.xiang.xiangaicodemother.config.properties;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "code")
@Data
public class CodeDeployProperties {

    private String deployHost = "http://localhost";

    public String normalizedDeployHost() {
        String host = StrUtil.blankToDefault(deployHost, "http://localhost").trim();
        return host.replaceAll("/+$", "");
    }
}
