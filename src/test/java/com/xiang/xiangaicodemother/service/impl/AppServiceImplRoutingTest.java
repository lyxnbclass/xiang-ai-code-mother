package com.xiang.xiangaicodemother.service.impl;

import com.xiang.xiangaicodemother.ai.AiCodeGenTypeRoutingService;
import com.xiang.xiangaicodemother.ai.model.CodeGenTypeRoutingResult;
import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppServiceImplRoutingTest {

    private final AiCodeGenTypeRoutingService routingService = mock(AiCodeGenTypeRoutingService.class);

    private final AppServiceImpl appService = new AppServiceImpl();

    @Test
    void shouldUseAiRoutingResult() {
        ReflectionTestUtils.setField(appService, "aiCodeGenTypeRoutingService", routingService);
        CodeGenTypeRoutingResult result = new CodeGenTypeRoutingResult();
        result.setCodeGenType(CodeGenTypeEnum.MULTI_FILE);
        when(routingService.routeCodeGenType("企业官网多个页面")).thenReturn(result);

        CodeGenTypeEnum selected = ReflectionTestUtils.invokeMethod(
                appService, "selectCodeGenType", "企业官网多个页面");

        assertEquals(CodeGenTypeEnum.MULTI_FILE, selected);
    }

    @Test
    void shouldFallbackWhenAiRoutingFails() {
        ReflectionTestUtils.setField(appService, "aiCodeGenTypeRoutingService", routingService);
        when(routingService.routeCodeGenType("电商后台管理系统"))
                .thenThrow(new IllegalStateException("model unavailable"));

        CodeGenTypeEnum selected = ReflectionTestUtils.invokeMethod(
                appService, "selectCodeGenType", "电商后台管理系统");

        assertEquals(CodeGenTypeEnum.VUE_PROJECT, selected);
    }
}
