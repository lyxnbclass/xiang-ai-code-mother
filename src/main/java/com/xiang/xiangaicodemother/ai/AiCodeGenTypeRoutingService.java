package com.xiang.xiangaicodemother.ai;

import com.xiang.xiangaicodemother.ai.model.CodeGenTypeRoutingResult;
import dev.langchain4j.service.SystemMessage;

/**
 * 根据用户需求智能选择代码生成类型。
 */
public interface AiCodeGenTypeRoutingService {

    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    CodeGenTypeRoutingResult routeCodeGenType(String userPrompt);
}
