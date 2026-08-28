package com.xiang.xiangaicodemother.monitor;

import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiModelMonitorListenerTest {

    private final AiModelMetricsCollector collector = mock(AiModelMetricsCollector.class);
    private final AiModelMonitorListener listener = new AiModelMonitorListener(collector);

    @AfterEach
    void clearContext() {
        MonitorContextHolder.clearContext();
    }

    @Test
    void shouldCaptureBusinessContextOnRequest() {
        ChatModelRequestContext requestContext = mock(ChatModelRequestContext.class);
        ChatRequest chatRequest = mock(ChatRequest.class);
        Map<Object, Object> attributes = new ConcurrentHashMap<>();
        when(requestContext.attributes()).thenReturn(attributes);
        when(requestContext.chatRequest()).thenReturn(chatRequest);
        when(chatRequest.modelName()).thenReturn("deepseek-chat");
        MonitorContext expected = new MonitorContext("10", "20");
        MonitorContextHolder.setContext(expected);

        listener.onRequest(requestContext);

        assertEquals(expected, attributes.get(AiModelMonitorListener.MONITOR_CONTEXT_KEY));
        assertNotNull(attributes.get(AiModelMonitorListener.REQUEST_START_TIME_KEY));
        verify(collector).recordRequest("10", "20", "deepseek-chat", "started");
    }

    @Test
    void shouldUseUnknownContextOutsideBusinessRequest() {
        ChatModelRequestContext requestContext = mock(ChatModelRequestContext.class);
        ChatRequest chatRequest = mock(ChatRequest.class);
        when(requestContext.attributes()).thenReturn(new ConcurrentHashMap<>());
        when(requestContext.chatRequest()).thenReturn(chatRequest);
        when(chatRequest.modelName()).thenReturn("deepseek-chat");

        listener.onRequest(requestContext);

        verify(collector).recordRequest(
                "unknown", "unknown", "deepseek-chat", "started");
    }
}
