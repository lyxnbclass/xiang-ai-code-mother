package com.xiang.xiangaicodemother.ai;

import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AiCodeGeneratorServiceFactoryTest {

    private final AiCodeGeneratorServiceFactory factory = new AiCodeGeneratorServiceFactory();

    @Test
    void shouldRejectInvalidAppIdBeforeAccessingInfrastructure() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.getAiCodeGeneratorService(0, CodeGenTypeEnum.HTML));
    }

    @Test
    void shouldRejectMissingCodeGenerationType() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.getAiCodeGeneratorService(1, null));
    }
}
