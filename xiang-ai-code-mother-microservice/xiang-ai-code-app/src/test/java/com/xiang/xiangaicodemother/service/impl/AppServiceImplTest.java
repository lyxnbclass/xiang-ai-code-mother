package com.xiang.xiangaicodemother.service.impl;

import com.xiang.xiangaicodemother.innerservice.InnerUserService;
import com.xiang.xiangaicodemother.model.entity.App;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.model.vo.AppVO;
import com.xiang.xiangaicodemother.model.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppServiceImplTest {

    @Test
    void getAppVOListShouldUseOneBatchUserRpc() {
        InnerUserService userService = mock(InnerUserService.class);
        AppServiceImpl appService = new AppServiceImpl();
        ReflectionTestUtils.setField(appService, "userService", userService);

        App firstApp = app(1L, 10L, "应用一");
        App secondApp = app(2L, 20L, "应用二");
        User firstUser = user(10L, "用户一");
        User secondUser = user(20L, "用户二");
        UserVO firstUserVO = userVO(10L, "用户一");
        UserVO secondUserVO = userVO(20L, "用户二");

        when(userService.listByIds(anyCollection())).thenReturn(List.of(firstUser, secondUser));
        when(userService.getUserVO(firstUser)).thenReturn(firstUserVO);
        when(userService.getUserVO(secondUser)).thenReturn(secondUserVO);

        List<AppVO> result = appService.getAppVOList(List.of(firstApp, secondApp));

        assertEquals(2, result.size());
        assertEquals("应用一", result.get(0).getAppName());
        assertEquals(10L, result.get(0).getUser().getId());
        assertEquals(20L, result.get(1).getUser().getId());
        verify(userService).listByIds(anyCollection());
        verify(userService, never()).getById(org.mockito.ArgumentMatchers.any());
    }

    private App app(long id, long userId, String appName) {
        App app = new App();
        app.setId(id);
        app.setUserId(userId);
        app.setAppName(appName);
        return app;
    }

    private User user(long id, String userName) {
        User user = new User();
        user.setId(id);
        user.setUserName(userName);
        return user;
    }

    private UserVO userVO(long id, String userName) {
        UserVO userVO = new UserVO();
        userVO.setId(id);
        userVO.setUserName(userName);
        return userVO;
    }
}
