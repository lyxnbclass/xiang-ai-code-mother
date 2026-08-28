package com.xiang.xiangaicodemother.monitor;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModelMetricsCollectorTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AiModelMetricsCollector collector = new AiModelMetricsCollector(registry);

    @Test
    void shouldRecordRequestsTokensErrorsAndDuration() {
        collector.recordRequest("1", "2", "deepseek-chat", "success");
        collector.recordTokenUsage("1", "2", "deepseek-chat", "total", 120);
        collector.recordError("1", "2", "deepseek-chat", "TimeoutException");
        collector.recordResponseTime("1", "2", "deepseek-chat", Duration.ofMillis(250));

        assertEquals(1, registry.find(AiModelMetricsCollector.REQUEST_METRIC).counter().count());
        assertEquals(120, registry.find(AiModelMetricsCollector.TOKEN_METRIC).counter().count());
        assertEquals(1, registry.find(AiModelMetricsCollector.ERROR_METRIC).counter().count());
        assertEquals(1, registry.find(AiModelMetricsCollector.RESPONSE_TIME_METRIC).timer().count());
    }

    @Test
    void shouldIgnoreNonPositiveTokenCountsAndNormalizeTags() {
        collector.recordTokenUsage("1", "2", "model", "total", 0);
        collector.recordRequest(null, "2", "model", "success");

        assertNull(registry.find(AiModelMetricsCollector.TOKEN_METRIC).counter());
        assertNotNull(registry.find(AiModelMetricsCollector.REQUEST_METRIC)
                .tag("user_id", "unknown").counter());
    }

    @Test
    void shouldExportDashboardCompatiblePrometheusNames() {
        PrometheusMeterRegistry prometheusRegistry =
                new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        AiModelMetricsCollector prometheusCollector =
                new AiModelMetricsCollector(prometheusRegistry);

        prometheusCollector.recordRequest("1", "2", "model", "success");
        prometheusCollector.recordTokenUsage("1", "2", "model", "total", 10);
        prometheusCollector.recordError("1", "2", "model", "TimeoutException");
        prometheusCollector.recordResponseTime(
                "1", "2", "model", Duration.ofMillis(100));

        String scrape = prometheusRegistry.scrape();
        assertTrue(scrape.contains("ai_model_requests_total"));
        assertTrue(scrape.contains("ai_model_tokens_total"));
        assertTrue(scrape.contains("ai_model_errors_total"));
        assertTrue(scrape.contains("ai_model_response_duration_seconds_count"));
        assertTrue(scrape.contains("error_type=\"TimeoutException\""));
    }
}
