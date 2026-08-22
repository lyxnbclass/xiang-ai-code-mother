package com.xiang.xiangaicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xiang.xiangaicodemother.model.entity.App;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface AppMapper extends BaseMapper<App> {

    @Update("UPDATE app SET cover = #{cover} WHERE id = #{appId} AND isDelete = 0")
    int updateCoverById(@Param("appId") Long appId, @Param("cover") String cover);
}
