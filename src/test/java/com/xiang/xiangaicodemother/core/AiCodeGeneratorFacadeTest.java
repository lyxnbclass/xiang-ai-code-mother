package com.xiang.xiangaicodemother.core;

import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("帮我做一个戴源和林宇翔的情侣相册，不超过20行", CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }
}
