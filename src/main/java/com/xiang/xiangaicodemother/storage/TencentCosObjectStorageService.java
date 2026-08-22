package com.xiang.xiangaicodemother.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.xiang.xiangaicodemother.config.properties.CosClientProperties;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 腾讯云 COS 对象存储实现。
 */
@Slf4j
@RequiredArgsConstructor
public class TencentCosObjectStorageService implements ObjectStorageService {

    private final COSClient cosClient;

    private final CosClientProperties properties;

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String upload(String objectKey, File file) {
        try {
            String normalizedKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
            cosClient.putObject(new PutObjectRequest(properties.getBucket(), normalizedKey, file));
            String host = properties.getHost().replaceAll("/+$", "");
            String url = host + "/" + normalizedKey;
            log.info("文件已上传到对象存储，key={}", normalizedKey);
            return url;
        } catch (Exception e) {
            log.error("上传文件到对象存储失败，key={}", objectKey, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传文件到对象存储失败");
        }
    }
}
