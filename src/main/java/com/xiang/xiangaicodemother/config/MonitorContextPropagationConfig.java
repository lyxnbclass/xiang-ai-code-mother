package com.xiang.xiangaicodemother.config;

import com.xiang.xiangaicodemother.monitor.MonitorContextHolder;
import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * 将 Reactor Context 中的监控维度安全传播到 LangChain4j 回调线程。
 */
@Configuration(proxyBeanMethods = false)
public class MonitorContextPropagationConfig {

    @PostConstruct
    public void initialize() {
        ContextRegistry registry = ContextRegistry.getInstance();
        registry.removeThreadLocalAccessor(MonitorContextHolder.CONTEXT_KEY);
        registry.registerThreadLocalAccessor(
                MonitorContextHolder.CONTEXT_KEY,
                MonitorContextHolder::getContext,
                MonitorContextHolder::setContext,
                MonitorContextHolder::clearContext);
        Hooks.enableAutomaticContextPropagation();
    }

    @PreDestroy
    public void destroy() {
        ContextRegistry.getInstance().removeThreadLocalAccessor(MonitorContextHolder.CONTEXT_KEY);
        MonitorContextHolder.clearContext();
    }
}
