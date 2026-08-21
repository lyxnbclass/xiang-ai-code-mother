package com.xiang.xiangaicodemother.ai.model.message;

import lombok.Getter;

@Getter
public enum StreamMessageTypeEnum {
    AI_RESPONSE("ai_response"),
    TOOL_REQUEST("tool_request"),
    TOOL_EXECUTED("tool_executed");

    private final String value;

    StreamMessageTypeEnum(String value) {
        this.value = value;
    }

    public static StreamMessageTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (StreamMessageTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
