package com.xiang.xiangaicodemother.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 将 LangChain4j 模型事件转换为 Micrometer 指标。
 */
@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    static final String REQUEST_START_TIME_KEY = "ai_monitor_request_start_time";
    static final String MONITOR_CONTEXT_KEY = "ai_monitor_context";

    private final AiModelMetricsCollector metricsCollector;

    public AiModelMonitorListener(AiModelMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        try {
            Map<Object, Object> attributes = requestContext.attributes();
            attributes.put(REQUEST_START_TIME_KEY, Instant.now());
            MonitorContext context = MonitorContextHolder.getContextOrUnknown();
            attributes.put(MONITOR_CONTEXT_KEY, context);
            metricsCollector.recordRequest(
                    context.userId(), context.appId(), requestContext.chatRequest().modelName(), "started");
        } catch (Exception e) {
            log.warn("记录 AI 请求指标失败", e);
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        try {
            Map<Object, Object> attributes = responseContext.attributes();
            MonitorContext context = contextFrom(attributes);
            String modelName = responseContext.chatResponse().modelName();
            metricsCollector.recordRequest(
                    context.userId(), context.appId(), modelName, "success");
            recordResponseTime(attributes, context, modelName);
            recordTokenUsage(responseContext, context, modelName);
        } catch (Exception e) {
            log.warn("记录 AI 响应指标失败", e);
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        try {
            Map<Object, Object> attributes = errorContext.attributes();
            MonitorContext context = contextFrom(attributes);
            String modelName = errorContext.chatRequest().modelName();
            Throwable error = errorContext.error();
            String errorType = error == null ? "UnknownError" : error.getClass().getSimpleName();
            metricsCollector.recordRequest(
                    context.userId(), context.appId(), modelName, "error");
            metricsCollector.recordError(
                    context.userId(), context.appId(), modelName, errorType);
            recordResponseTime(attributes, context, modelName);
        } catch (Exception e) {
            log.warn("记录 AI 错误指标失败", e);
        }
    }

    private MonitorContext contextFrom(Map<Object, Object> attributes) {
        Object context = attributes.get(MONITOR_CONTEXT_KEY);
        return context instanceof MonitorContext monitorContext
                ? monitorContext : MonitorContext.UNKNOWN;
    }

    private void recordResponseTime(Map<Object, Object> attributes,
                                    MonitorContext context, String modelName) {
        Object startTime = attributes.get(REQUEST_START_TIME_KEY);
        if (startTime instanceof Instant instant) {
            metricsCollector.recordResponseTime(
                    context.userId(), context.appId(), modelName,
                    Duration.between(instant, Instant.now()));
        }
    }

    private void recordTokenUsage(ChatModelResponseContext responseContext,
                                  MonitorContext context, String modelName) {
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        if (tokenUsage == null) {
            return;
        }
        metricsCollector.recordTokenUsage(
                context.userId(), context.appId(), modelName, "input", tokenUsage.inputTokenCount());
        metricsCollector.recordTokenUsage(
                context.userId(), context.appId(), modelName, "output", tokenUsage.outputTokenCount());
        metricsCollector.recordTokenUsage(
                context.userId(), context.appId(), modelName, "total", tokenUsage.totalTokenCount());
    }
}
