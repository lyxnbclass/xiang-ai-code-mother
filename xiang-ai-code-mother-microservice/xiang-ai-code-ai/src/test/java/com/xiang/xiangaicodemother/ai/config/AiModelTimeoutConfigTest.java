package com.xiang.xiangaicodemother.ai.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiModelTimeoutConfigTest {

    @Test
    void allManuallyBuiltModelsShouldDefaultToFiveMinutes() {
        Duration expected = Duration.ofSeconds(300);

        assertEquals(expected, new StreamingChatModelConfig().getTimeout());
        assertEquals(expected, new ReasoningStreamingChatModelConfig().getTimeout());
        assertEquals(expected, new RoutingAiModelConfig().getTimeout());
    }
}
