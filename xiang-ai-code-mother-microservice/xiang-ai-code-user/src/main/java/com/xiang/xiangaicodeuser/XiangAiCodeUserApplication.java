package com.xiang.xiangaicodeuser;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDubbo
@MapperScan("com.xiang.xiangaicodeuser.mapper")
@ComponentScan("com.xiang")
public class XiangAiCodeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(XiangAiCodeUserApplication.class, args);
    }
}