package com.xiang.xiangaicodemother.config.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiChatMemoryPropertiesTest {

    @Test
    void shouldDefaultToFiftyMessages() {
        assertEquals(50, new AiChatMemoryProperties().getMaxMessages());
    }

    @Test
    void shouldRejectNonPositiveWindowSize() {
        AiChatMemoryProperties properties = new AiChatMemoryProperties();

        assertThrows(IllegalArgumentException.class, () -> properties.setMaxMessages(0));
        assertThrows(IllegalArgumentException.class, () -> properties.setMaxMessages(-1));
    }
}
