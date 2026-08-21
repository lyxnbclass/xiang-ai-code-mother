package com.xiang.xiangaicodemother.ai.model.message;

import dev.langchain4j.service.tool.ToolExecution;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolExecutedMessage extends StreamMessage {
    private String id;
    private String name;
    private String arguments;
    private String result;

    public ToolExecutedMessage(ToolExecution execution) {
        super(StreamMessageTypeEnum.TOOL_EXECUTED.getValue());
        this.id = execution.request().id();
        this.name = execution.request().name();
        this.arguments = execution.request().arguments();
        this.result = execution.result();
    }
}
