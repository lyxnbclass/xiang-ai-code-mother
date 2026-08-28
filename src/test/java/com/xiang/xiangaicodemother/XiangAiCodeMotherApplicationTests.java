package com.xiang.xiangaicodemother;

import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotSame;

@SpringBootTest(properties = {
        "DEEPSEEK_API_KEY=test-key",
        "spring.datasource.url=jdbc:h2:mem:context-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "spring.batch.jdbc.initialize-schema=never"
})
class XiangAiCodeMotherApplicationTests {

    @Resource
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
    }

    @Test
    void streamingModelsUsePrototypeScope() {
        StreamingChatModel first = applicationContext.getBean(
                "streamingChatModelPrototype", StreamingChatModel.class);
        StreamingChatModel second = applicationContext.getBean(
                "streamingChatModelPrototype", StreamingChatModel.class);

        assertNotSame(first, second);
    }

}
