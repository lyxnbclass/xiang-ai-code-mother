package com.xiang.xiangaicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 返回给前端的应用信息。
 */
@Data
public class AppVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String appName;
    private String cover;
    private String initPrompt;
    private String codeGenType;
    private String deployKey;
    private LocalDateTime deployedTime;
    private Integer priority;
    private Long userId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private UserVO user;
}
