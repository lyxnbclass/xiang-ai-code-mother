package com.xiang.xiangaicodemother.ai;

import com.xiang.xiangaicodemother.ai.tools.FileWriteTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class VueCodeGeneratorServiceTest {

    @Test
    void supportsMemoryIdAndFileToolConfiguration() {
        StreamingChatModel model = mock(StreamingChatModel.class);

        VueCodeGeneratorService service = AiServices.builder(VueCodeGeneratorService.class)
                .streamingChatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .tools(new FileWriteTool())
                .build();

        assertNotNull(service);
    }
}
