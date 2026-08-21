package com.xiang.xiangaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.xiang.xiangaicodemother.model.dto.app.AppQueryRequest;
import com.xiang.xiangaicodemother.model.dto.app.AppAddRequest;
import com.xiang.xiangaicodemother.model.entity.App;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AppService extends IService<App> {

    Long createApp(AppAddRequest appAddRequest, User loginUser);

    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    String deployApp(Long appId, User loginUser);

    AppVO getAppVO(App app);

    List<AppVO> getAppVOList(List<App> appList);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
}
