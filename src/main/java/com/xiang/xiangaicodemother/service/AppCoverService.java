package com.xiang.xiangaicodemother.service;

/**
 * 应用封面异步生成服务。
 */
public interface AppCoverService {

    void generateAppCoverAsync(Long appId, String appUrl);
}
