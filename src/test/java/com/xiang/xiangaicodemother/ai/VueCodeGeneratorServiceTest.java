package com.xiang.xiangaicodemother.ai;

import com.xiang.xiangaicodemother.ai.tools.BaseTool;
import com.xiang.xiangaicodemother.ai.tools.FileDeleteTool;
import com.xiang.xiangaicodemother.ai.tools.FileDirReadTool;
import com.xiang.xiangaicodemother.ai.tools.FileModifyTool;
import com.xiang.xiangaicodemother.ai.tools.FileReadTool;
import com.xiang.xiangaicodemother.ai.tools.FileWriteTool;
import com.xiang.xiangaicodemother.ai.tools.ToolManager;
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
        ToolManager toolManager = new ToolManager(new BaseTool[]{
                new FileWriteTool(), new FileReadTool(), new FileModifyTool(),
                new FileDeleteTool(), new FileDirReadTool()
        });

        VueCodeGeneratorService service = AiServices.builder(VueCodeGeneratorService.class)
                .streamingChatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .tools((Object[]) toolManager.getAllTools())
                .build();

        assertNotNull(service);
    }
}
