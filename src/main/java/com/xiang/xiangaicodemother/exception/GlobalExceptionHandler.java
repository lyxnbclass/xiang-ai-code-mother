package com.xiang.xiangaicodemother.exception;

import cn.hutool.json.JSONUtil;
import com.xiang.xiangaicodemother.common.BaseResponse;
import com.xiang.xiangaicodemother.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        log.error("businessException: ", e);
        if (writeSseError(request, response, e.getCode(), e.getMessage())) {
            return null;
        }
        return ResultUtils.error(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        log.error("runtimeException: ", e);
        if (writeSseError(request, response, ErrorCode.SYSTEM_ERROR.getCode(), "系统错误")) {
            return null;
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"系统错误");
    }

    private boolean writeSseError(HttpServletRequest request, HttpServletResponse response,
                                  int code, String message) {
        String accept = request.getHeader("Accept");
        boolean sseRequest = (accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE))
                || request.getRequestURI().endsWith("/app/chat/gen/code");
        if (!sseRequest) {
            return false;
        }
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Cache-Control", "no-cache");
            String data = JSONUtil.toJsonStr(Map.of("code", code, "message", message));
            response.getWriter().write("event: business-error\ndata: " + data + "\n\n");
            response.getWriter().flush();
        } catch (IOException ioException) {
            log.warn("写入 SSE 错误事件失败", ioException);
        }
        return true;
    }
}
