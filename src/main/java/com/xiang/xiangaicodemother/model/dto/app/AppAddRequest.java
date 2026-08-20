package com.xiang.xiangaicodemother.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String initPrompt;
}
