package com.xiang.xiangaicodemother.core.handler;

import cn.hutool.core.util.StrUtil;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.xiang.xiangaicodemother.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public class SimpleTextStreamHandler {

    public Flux<String> handle(Flux<String> originFlux, ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        StringBuilder response = new StringBuilder();
        return originFlux
                .doOnNext(response::append)
                .doOnComplete(() -> save(chatHistoryService, appId, response.toString(), loginUser.getId()))
                .doOnError(error -> save(chatHistoryService, appId,
                        "AI 回复失败：" + StrUtil.blankToDefault(error.getMessage(), "未知错误"),
                        loginUser.getId()));
    }

    private void save(ChatHistoryService service, long appId, String message, long userId) {
        if (StrUtil.isBlank(message)) {
            log.warn("AI 返回空消息，appId={}", appId);
            return;
        }
        if (!service.addChatMessage(appId, message,
                ChatHistoryMessageTypeEnum.AI.getValue(), userId)) {
            log.error("保存 AI 消息失败，appId={}", appId);
        }
    }
}
