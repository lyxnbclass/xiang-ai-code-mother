package com.xiang.xiangaicodemother.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 对话消息类型。
 */
@Getter
public enum ChatHistoryMessageTypeEnum {

    USER("用户", "user"),
    AI("AI", "ai");

    private final String text;
    private final String value;

    ChatHistoryMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ChatHistoryMessageTypeEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (ChatHistoryMessageTypeEnum messageType : values()) {
            if (messageType.value.equals(value)) {
                return messageType;
            }
        }
        return null;
    }
}
