package com.xiang.xiangaicodemother.model.dto.chathistory;

import com.xiang.xiangaicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String message;
    private String messageType;
    private Long appId;
    private Long userId;
    private LocalDateTime lastCreateTime;
}
