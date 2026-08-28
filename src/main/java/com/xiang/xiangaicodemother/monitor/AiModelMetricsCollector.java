package com.xiang.xiangaicodemother.monitor;

import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 收集 AI 模型调用次数、错误、Token 消耗和响应耗时。
 */
@Component
public class AiModelMetricsCollector {

    static final String REQUEST_METRIC = "ai_model_requests";
    static final String ERROR_METRIC = "ai_model_errors";
    static final String TOKEN_METRIC = "ai_model_tokens";
    static final String RESPONSE_TIME_METRIC = "ai_model_response_duration";

    private static final String UNKNOWN = "unknown";
    private static final int MAX_TAG_LENGTH = 80;

    private final MeterRegistry meterRegistry;

    public AiModelMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequest(String userId, String appId, String modelName, String status) {
        Counter.builder(REQUEST_METRIC)
                .description("AI 模型请求次数")
                .tag("user_id", safeTag(userId))
                .tag("app_id", safeTag(appId))
                .tag("model_name", safeTag(modelName))
                .tag("status", safeTag(status))
                .register(meterRegistry)
                .increment();
    }

    public void recordError(String userId, String appId, String modelName, String errorType) {
        Counter.builder(ERROR_METRIC)
                .description("AI 模型错误次数")
                .tag("user_id", safeTag(userId))
                .tag("app_id", safeTag(appId))
                .tag("model_name", safeTag(modelName))
                .tag("error_type", safeTag(errorType))
                .register(meterRegistry)
                .increment();
    }

    public void recordTokenUsage(String userId, String appId, String modelName,
                                 String tokenType, long tokenCount) {
        if (tokenCount <= 0) {
            return;
        }
        Counter.builder(TOKEN_METRIC)
                .description("AI 模型 Token 消耗")
                .tag("user_id", safeTag(userId))
                .tag("app_id", safeTag(appId))
                .tag("model_name", safeTag(modelName))
                .tag("token_type", safeTag(tokenType))
                .register(meterRegistry)
                .increment(tokenCount);
    }

    public void recordResponseTime(String userId, String appId, String modelName, Duration duration) {
        if (duration == null || duration.isNegative()) {
            return;
        }
        Timer.builder(RESPONSE_TIME_METRIC)
                .description("AI 模型响应时间")
                .tag("user_id", safeTag(userId))
                .tag("app_id", safeTag(appId))
                .tag("model_name", safeTag(modelName))
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(duration);
    }

    private String safeTag(String value) {
        String tag = StrUtil.blankToDefault(value, UNKNOWN).trim();
        return StrUtil.maxLength(tag, MAX_TAG_LENGTH);
    }
}
