package com.xiang.xiangaicodemother.workflow.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiang.xiangaicodemother.config.properties.WorkflowProperties;
import com.xiang.xiangaicodemother.workflow.model.ImageCategoryEnum;
import com.xiang.xiangaicodemother.workflow.model.ImageResource;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 可配置的 unDraw 插画搜索；接口变化时可通过环境变量替换模板。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IllustrationSearchTool {
    private final WorkflowProperties properties;

    @Tool("搜索插画图片，用于网站美化和装饰")
    public List<ImageResource> search(@P("搜索关键词") String query) {
        String template = properties.getImages().getUndrawUrlTemplate();
        if (StrUtil.isBlank(template) || StrUtil.isBlank(query)) {
            return List.of();
        }
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url;
        try {
            url = String.format(template, encoded, encoded);
        } catch (Exception e) {
            log.warn("unDraw URL 模板无效");
            return List.of();
        }
        List<ImageResource> images = new ArrayList<>();
        try (HttpResponse response = HttpRequest.get(url).timeout(10_000).execute()) {
            if (!response.isOk()) {
                return List.of();
            }
            JSONObject pageProps = JSONUtil.parseObj(response.body()).getJSONObject("pageProps");
            JSONArray results = pageProps == null ? null : pageProps.getJSONArray("initialResults");
            if (results == null && pageProps != null) {
                results = pageProps.getJSONArray("illustrations");
            }
            if (results == null) {
                return List.of();
            }
            int count = Math.min(results.size(), Math.max(1, properties.getImages().getMaxResultsPerTask()));
            for (int i = 0; i < count; i++) {
                JSONObject illustration = results.getJSONObject(i);
                String media = illustration.getStr("media");
                if (ContentImageSearchTool.isHttpUrl(media)) {
                    images.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(illustration.getStr("title", query))
                            .url(media)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("unDraw 搜索异常: {}", e.getMessage());
        }
        return images;
    }
}
