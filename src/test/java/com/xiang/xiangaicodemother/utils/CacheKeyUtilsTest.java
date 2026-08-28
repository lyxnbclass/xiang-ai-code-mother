package com.xiang.xiangaicodemother.utils;

import com.xiang.xiangaicodemother.model.dto.app.AppQueryRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CacheKeyUtilsTest {

    @Test
    void createsStableKeyAndIncludesQueryValues() {
        AppQueryRequest first = new AppQueryRequest();
        first.setPageNum(1);
        first.setPageSize(20);
        AppQueryRequest same = new AppQueryRequest();
        same.setPageNum(1);
        same.setPageSize(20);
        AppQueryRequest nextPage = new AppQueryRequest();
        nextPage.setPageNum(2);
        nextPage.setPageSize(20);

        assertEquals(CacheKeyUtils.generateKey(first), CacheKeyUtils.generateKey(same));
        assertNotEquals(CacheKeyUtils.generateKey(first), CacheKeyUtils.generateKey(nextPage));
    }
}
