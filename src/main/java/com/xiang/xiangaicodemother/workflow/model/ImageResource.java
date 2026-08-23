package com.xiang.xiangaicodemother.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/** 工作流收集到的图片资源。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResource implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private ImageCategoryEnum category;
    private String description;
    private String url;
}
