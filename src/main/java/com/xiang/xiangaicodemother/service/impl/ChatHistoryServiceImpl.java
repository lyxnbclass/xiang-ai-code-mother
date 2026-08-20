package com.xiang.xiangaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xiang.xiangaicodemother.constant.UserConstant;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.exception.ThrowUtils;
import com.xiang.xiangaicodemother.mapper.ChatHistoryMapper;
import com.xiang.xiangaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.xiang.xiangaicodemother.model.entity.App;
import com.xiang.xiangaicodemother.model.entity.ChatHistory;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.xiang.xiangaicodemother.service.AppService;
import com.xiang.xiangaicodemother.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>
        implements ChatHistoryService {

    private static final int MAX_PAGE_SIZE = 50;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "messageType", "appId", "userId", "createTime", "updateTime"
    );

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        ThrowUtils.throwIf(ChatHistoryMessageTypeEnum.getEnumByValue(messageType) == null,
                ErrorCode.PARAMS_ERROR, "不支持的消息类型");

        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        return this.remove(QueryWrapper.create().eq("appId", appId));
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "页面大小必须在 1-50 之间");
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null,
                ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");

        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        return this.page(Page.of(1, pageSize), getQueryWrapper(queryRequest));
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        if (appId == null || appId <= 0 || chatMemory == null || maxCount <= 0) {
            return 0;
        }
        try {
            // 当前用户消息已先写入数据库、稍后还会由 AI Service 加入记忆，因此跳过最新一条防止重复。
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq("appId", appId)
                    .orderBy("createTime", false)
                    .orderBy("id", false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }

            historyList = historyList.reversed();
            chatMemory.clear();
            int loadedCount = 0;
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("已为 appId={} 加载 {} 条历史消息到对话记忆", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId={}", appId, e);
            return 0;
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest request) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (request == null) {
            return queryWrapper.orderBy("createTime", false).orderBy("id", false);
        }

        queryWrapper.eq("id", request.getId(), request.getId() != null)
                .like("message", request.getMessage(), StrUtil.isNotBlank(request.getMessage()))
                .eq("messageType", request.getMessageType(), StrUtil.isNotBlank(request.getMessageType()))
                .eq("appId", request.getAppId(), request.getAppId() != null)
                .eq("userId", request.getUserId(), request.getUserId() != null);
        if (request.getLastCreateTime() != null) {
            queryWrapper.lt("createTime", request.getLastCreateTime());
        }

        String sortField = request.getSortField();
        if (StrUtil.isNotBlank(sortField) && ALLOWED_SORT_FIELDS.contains(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(request.getSortOrder()));
        } else {
            queryWrapper.orderBy("createTime", false).orderBy("id", false);
        }
        return queryWrapper;
    }
}
