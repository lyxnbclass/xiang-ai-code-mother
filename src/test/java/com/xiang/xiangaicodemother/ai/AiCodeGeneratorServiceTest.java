package com.xiang.xiangaicodemother.ai;

import com.xiang.xiangaicodemother.ai.model.HtmlCodeResult;
import com.xiang.xiangaicodemother.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("帮我做一个戴源和林宇翔的情侣相册，不超过20行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("帮我做一个戴源和林宇翔的情侣空间，不超过20行");
        Assertions.assertNotNull(result);
    }
}
