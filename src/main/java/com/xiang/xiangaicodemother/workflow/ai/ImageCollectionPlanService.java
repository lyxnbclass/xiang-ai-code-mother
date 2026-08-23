package com.xiang.xiangaicodemother.workflow.ai;

import com.xiang.xiangaicodemother.workflow.model.ImageCollectionPlan;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/** 使用结构化输出规划图片收集任务。 */
public interface ImageCollectionPlanService {

    @SystemMessage(fromResource = "prompt/image-collection-plan-system-prompt.txt")
    ImageCollectionPlan plan(@UserMessage String prompt);
}
