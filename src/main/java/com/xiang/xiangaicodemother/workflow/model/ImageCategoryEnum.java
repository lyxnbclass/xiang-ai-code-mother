package com.xiang.xiangaicodemother.workflow.model;

import lombok.Getter;

/** 工作流图片类型。 */
@Getter
public enum ImageCategoryEnum {
    CONTENT("内容图片"),
    ILLUSTRATION("插画图片"),
    ARCHITECTURE("架构图"),
    LOGO("Logo 图片");

    private final String text;

    ImageCategoryEnum(String text) {
        this.text = text;
    }
}
