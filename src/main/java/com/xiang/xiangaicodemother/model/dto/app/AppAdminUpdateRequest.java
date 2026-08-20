package com.xiang.xiangaicodemother.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppAdminUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String appName;
    private String cover;
    private Integer priority;
}
