package com.xiang.xiangaicodemother.monitor;

import java.io.Serializable;

/**
 * 一次 AI 业务调用的监控维度。
 */
public record MonitorContext(String userId, String appId) implements Serializable {

    public static final MonitorContext UNKNOWN = new MonitorContext("unknown", "unknown");
}
