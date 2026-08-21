package com.xiang.xiangaicodemother.core.handler;

import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import com.xiang.xiangaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    public Flux<String> doExecute(Flux<String> originFlux, ChatHistoryService chatHistoryService,
                                  long appId, User loginUser, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case VUE_PROJECT -> jsonMessageStreamHandler.handle(
                    originFlux, chatHistoryService, appId, loginUser);
            case HTML, MULTI_FILE -> new SimpleTextStreamHandler().handle(
                    originFlux, chatHistoryService, appId, loginUser);
        };
    }
}
