package com.xiang.xiangaicodemother.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/** Vue 工程工具调用专用 AI 服务。 */
public interface VueCodeGeneratorService {

    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId,
                                              @UserMessage String userMessage);
}
