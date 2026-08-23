package com.xiang.xiangaicodemother.workflow.tool;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.xiang.xiangaicodemother.config.properties.WorkflowProperties;
import com.xiang.xiangaicodemother.workflow.model.ImageCategoryEnum;
import com.xiang.xiangaicodemother.workflow.model.ImageResource;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** DashScope Logo 生成。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogoGeneratorTool {
    private final WorkflowProperties properties;

    @Tool("根据描述生成不含文字的 Logo 图片")
    public List<ImageResource> generate(@P("Logo 设计描述") String description) {
        String apiKey = properties.getImages().getDashscopeApiKey();
        if (StrUtil.isBlank(apiKey) || StrUtil.isBlank(description)) {
            return List.of();
        }
        try {
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(apiKey)
                    .model(properties.getImages().getDashscopeModel())
                    .prompt("生成简洁专业的 Logo，图中禁止包含文字。设计要求：" + description)
                    .size("512*512")
                    .n(1)
                    .build();
            ImageSynthesisResult result = new ImageSynthesis().call(param);
            List<Map<String, String>> results = result == null || result.getOutput() == null
                    ? List.of() : result.getOutput().getResults();
            List<ImageResource> images = new ArrayList<>();
            if (results != null) {
                for (Map<String, String> item : results) {
                    String url = item.get("url");
                    if (ContentImageSearchTool.isHttpUrl(url)) {
                        images.add(ImageResource.builder().category(ImageCategoryEnum.LOGO)
                                .description(description).url(url).build());
                    }
                }
            }
            return images;
        } catch (Exception e) {
            log.warn("Logo 生成异常: {}", e.getMessage());
            return List.of();
        }
    }
}
