package com.xiang.xiangaicodemother.workflow.state;

import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import com.xiang.xiangaicodemother.workflow.model.ImageCollectionPlan;
import com.xiang.xiangaicodemother.workflow.model.ImageResource;
import com.xiang.xiangaicodemother.workflow.model.QualityResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** 在工作流节点之间传递的业务状态。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext implements Serializable {
    public static final String STATE_KEY = "workflowContext";

    @Serial
    private static final long serialVersionUID = 1L;

    private Long appId;
    private String currentStep;
    private String originalPrompt;
    private String enhancedPrompt;
    private CodeGenTypeEnum generationType;
    private ImageCollectionPlan imageCollectionPlan;
    private List<ImageResource> imageList;
    private String generatedCodeDir;
    private String buildResultDir;
    private QualityResult qualityResult;
    private int qualityRetryCount;
    private int maxQualityRetries;
    private boolean existingProject;

    public static WorkflowContext from(MessagesState<String> state) {
        Object value = state.data().get(STATE_KEY);
        if (!(value instanceof WorkflowContext context)) {
            throw new IllegalStateException("工作流上下文不存在");
        }
        return context;
    }

    public static Map<String, Object> save(WorkflowContext context) {
        return Map.of(STATE_KEY, context);
    }
}
