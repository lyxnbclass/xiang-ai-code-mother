package com.xiang.xiangaicodemother.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductionConfigurationTest {

    @Test
    void productionExampleShouldBeValidYaml() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(
                "production-example",
                new ClassPathResource("application-prod.example.yml"));

        assertFalse(propertySources.isEmpty());
        assertEquals("${APP_DEPLOY_HOST:http://localhost/dist}",
                propertySources.getFirst().getProperty("code.deploy-host"));
    }
}
