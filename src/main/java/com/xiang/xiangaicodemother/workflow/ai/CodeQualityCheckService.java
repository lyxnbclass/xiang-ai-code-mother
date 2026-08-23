package com.xiang.xiangaicodemother.workflow.ai;

import com.xiang.xiangaicodemother.workflow.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** 使用结构化输出检查生成代码。 */
public interface CodeQualityCheckService {

    @SystemMessage(fromResource = "prompt/code-quality-check-system-prompt.txt")
    @UserMessage("{{codeContent}}")
    QualityResult check(@V("codeContent") String codeContent);
}
