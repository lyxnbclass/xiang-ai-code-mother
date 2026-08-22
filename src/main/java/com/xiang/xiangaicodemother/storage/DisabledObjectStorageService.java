package com.xiang.xiangaicodemother.storage;

import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;

import java.io.File;

/**
 * 未启用对象存储时的安全降级实现。
 */
public class DisabledObjectStorageService implements ObjectStorageService {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String upload(String objectKey, File file) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "对象存储未启用");
    }
}
