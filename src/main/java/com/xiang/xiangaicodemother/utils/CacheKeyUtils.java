package com.xiang.xiangaicodemother.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

/** 为复杂查询对象生成长度固定的缓存键。 */
public final class CacheKeyUtils {

    private CacheKeyUtils() {
    }

    public static String generateKey(Object value) {
        return DigestUtil.sha256Hex(JSONUtil.toJsonStr(value));
    }
}
