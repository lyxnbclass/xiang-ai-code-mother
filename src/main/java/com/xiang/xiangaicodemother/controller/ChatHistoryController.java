package com.xiang.xiangaicodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.xiang.xiangaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.xiang.xiangaicodemother.annotation.AuthCheck;
import com.xiang.xiangaicodemother.common.BaseResponse;
import com.xiang.xiangaicodemother.common.DeleteRequest;
import com.xiang.xiangaicodemother.common.ResultUtils;
import com.xiang.xiangaicodemother.constant.UserConstant;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.exception.ThrowUtils;
import com.xiang.xiangaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.xiang.xiangaicodemother.model.entity.ChatHistory;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.service.ChatHistoryService;
import com.xiang.xiangaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(
            @PathVariable Long appId,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) LocalDateTime lastCreateTime,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(chatHistoryService.listAppChatHistoryByPage(
                appId, pageSize, lastCreateTime, loginUser));
    }

    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(
            @RequestBody ChatHistoryQueryRequest request) {
        ThrowUtils.throwIf(request == null || request.getPageNum() <= 0
                        || request.getPageSize() <= 0 || request.getPageSize() > 100,
                ErrorCode.PARAMS_ERROR, "分页参数错误，每页最多查询 100 条记录");
        return ResultUtils.success(chatHistoryService.page(
                Page.of(request.getPageNum(), request.getPageSize()),
                chatHistoryService.getQueryWrapper(request)));
    }

    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteChatHistoryByAdmin(@RequestBody DeleteRequest request) {
        Long id = request == null ? null : request.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "对话记录 ID 错误");
        ChatHistory chatHistory = chatHistoryService.getById(id);
        ThrowUtils.throwIf(chatHistory == null, ErrorCode.NOT_FOUND_ERROR, "对话记录不存在");
        boolean removed = chatHistoryService.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR, "删除对话记录失败");
        aiCodeGeneratorServiceFactory.clearAppChatMemory(chatHistory.getAppId());
        return ResultUtils.success(true);
    }
}
