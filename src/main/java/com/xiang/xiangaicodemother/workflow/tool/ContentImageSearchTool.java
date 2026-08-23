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

/** Pexels 内容图片搜索。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentImageSearchTool {
    private static final String API_URL = "https://api.pexels.com/v1/search";
    private final WorkflowProperties properties;

    @Tool("搜索内容相关图片，用于网站内容展示")
    public List<ImageResource> search(@P("搜索关键词") String query) {
        String apiKey = properties.getImages().getPexelsApiKey();
        if (StrUtil.isBlank(apiKey) || StrUtil.isBlank(query)) {
            return List.of();
        }
        int count = Math.max(1, Math.min(properties.getImages().getMaxResultsPerTask(), 12));
        String url = API_URL + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&per_page=" + count + "&page=1";
        List<ImageResource> images = new ArrayList<>();
        try (HttpResponse response = HttpRequest.get(url)
                .header("Authorization", apiKey)
                .timeout(10_000)
                .execute()) {
            if (!response.isOk()) {
                log.warn("Pexels 搜索失败，状态码={}", response.getStatus());
                return List.of();
            }
            JSONArray photos = JSONUtil.parseObj(response.body()).getJSONArray("photos");
            if (photos == null) {
                return List.of();
            }
            for (Object item : photos) {
                JSONObject photo = (JSONObject) item;
                JSONObject src = photo.getJSONObject("src");
                String imageUrl = src == null ? null : src.getStr("medium");
                if (isHttpUrl(imageUrl)) {
                    images.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)
                            .description(photo.getStr("alt", query))
                            .url(imageUrl)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Pexels 搜索异常: {}", e.getMessage());
        }
        return images;
    }

    static boolean isHttpUrl(String url) {
        return StrUtil.startWithAny(url, "https://", "http://");
    }
}
