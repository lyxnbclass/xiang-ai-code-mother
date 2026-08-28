package com.xiang.xiangaicodemother.monitor;

/**
 * 为 LangChain4j 监听器提供当前 Reactor 调用链的监控上下文。
 */
public final class MonitorContextHolder {

    public static final String CONTEXT_KEY = MonitorContext.class.getName();

    private static final ThreadLocal<MonitorContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private MonitorContextHolder() {
    }

    public static void setContext(MonitorContext context) {
        if (context == null) {
            clearContext();
            return;
        }
        CONTEXT_HOLDER.set(context);
    }

    public static MonitorContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    public static MonitorContext getContextOrUnknown() {
        MonitorContext context = getContext();
        return context == null ? MonitorContext.UNKNOWN : context;
    }

    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}
