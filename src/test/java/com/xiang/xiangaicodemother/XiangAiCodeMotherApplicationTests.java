package com.xiang.xiangaicodemother;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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

    @Test
    void contextLoads() {
    }

}
