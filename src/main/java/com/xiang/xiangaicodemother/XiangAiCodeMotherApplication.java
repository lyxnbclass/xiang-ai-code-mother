package com.xiang.xiangaicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = RedisEmbeddingStoreAutoConfiguration.class)
@MapperScan("com.xiang.xiangaicodemother.mapper")
public class XiangAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiangAiCodeMotherApplication.class, args);
    }

}
