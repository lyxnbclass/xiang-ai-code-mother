package com.xiang.xiangaicodemother;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xiang.xiangaicodemother.mapper")
public class XiangAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiangAiCodeMotherApplication.class, args);
    }

}
