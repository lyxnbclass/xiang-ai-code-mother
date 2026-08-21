package com.xiang.xiangaicodemother.storage;

import java.io.File;

/**
 * 对象存储通用能力。
 */
public interface ObjectStorageService {

    boolean isAvailable();

    String upload(String objectKey, File file);
}
