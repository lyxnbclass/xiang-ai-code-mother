package com.xiang.xiangaicodemother.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** AI 工作流配置。 */
@Data
@ConfigurationProperties(prefix = "workflow")
public class WorkflowProperties {

    private int maxQualityRetries = 1;

    private Images images = new Images();

    @Data
    public static class Images {
        private String pexelsApiKey;
        private String undrawUrlTemplate;
        private String dashscopeApiKey;
        private String dashscopeModel = "wan2.2-t2i-flash";
        private int maxResultsPerTask = 6;
    }
}
