package com.xiang.xiangaicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.xiang.xiangaicodemother.mapper")
@EnableDubbo
@EnableCaching
public class XiangAiCodeAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(XiangAiCodeAppApplication.class, args);
    }
}