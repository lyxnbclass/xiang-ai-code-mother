package com.xiang.xiangaicodemother.ai.model.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolRequestMessage extends StreamMessage {
    private String id;
    private String name;
    private String arguments;

    public ToolRequestMessage(ToolExecutionRequest request) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = request.id();
        this.name = request.name();
        this.arguments = request.arguments();
    }
}
