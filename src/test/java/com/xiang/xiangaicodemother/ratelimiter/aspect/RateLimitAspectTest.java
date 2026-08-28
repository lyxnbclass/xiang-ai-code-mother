package com.xiang.xiangaicodemother.ratelimiter.aspect;

import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.ratelimiter.annotation.RateLimit;
import com.xiang.xiangaicodemother.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fallsBackToLocalLimitWhenRedisIsUnavailable() throws Throwable {
        ObjectProvider<RedissonClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        UserService userService = mock(UserService.class);
        User user = new User();
        user.setId(7L);
        when(userService.getLoginUser(org.mockito.ArgumentMatchers.any())).thenReturn(user);
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.proceed()).thenReturn("ok");
        RateLimit rateLimit = LimitedEndpoint.class.getDeclaredMethod("invoke")
                .getAnnotation(RateLimit.class);
        RateLimitAspect aspect = new RateLimitAspect(provider, userService);

        for (int i = 0; i < 5; i++) {
            assertEquals("ok", aspect.enforce(point, rateLimit));
        }
        BusinessException exception = assertThrows(BusinessException.class,
                () -> aspect.enforce(point, rateLimit));

        assertEquals(ErrorCode.TOO_MANY_REQUEST.getCode(), exception.getCode());
        verify(point, times(5)).proceed();
    }

    private static class LimitedEndpoint {
        @RateLimit(rate = 5, rateInterval = 60)
        void invoke() {
        }
    }
}
