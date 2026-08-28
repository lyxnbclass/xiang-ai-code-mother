package com.xiang.xiangaicodemother.monitor;

import com.xiang.xiangaicodemother.config.MonitorContextPropagationConfig;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonitorContextPropagationConfigTest {

    @Test
    void shouldRestoreThreadLocalFromReactorContext() {
        MonitorContextPropagationConfig config = new MonitorContextPropagationConfig();
        config.initialize();
        MonitorContext expected = new MonitorContext("1", "2");
        AtomicReference<MonitorContext> actual = new AtomicReference<>();
        try {
            Mono.just("value")
                    .doOnNext(value -> actual.set(MonitorContextHolder.getContext()))
                    .contextWrite(context -> context.put(MonitorContextHolder.CONTEXT_KEY, expected))
                    .block();
        } finally {
            config.destroy();
        }

        assertEquals(expected, actual.get());
    }
}
