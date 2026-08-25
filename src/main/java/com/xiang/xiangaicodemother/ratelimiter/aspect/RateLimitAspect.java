package com.xiang.xiangaicodemother.ratelimiter.aspect;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.ratelimiter.annotation.RateLimit;
import com.xiang.xiangaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/** 优先使用 Redis 分布式令牌桶，连接异常时以进程内窗口限流兜底。 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAspect {

    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final UserService userService;
    private final Cache<String, LocalWindow> localWindows = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    public RateLimitAspect(ObjectProvider<RedissonClient> redissonClientProvider,
                           UserService userService) {
        this.redissonClientProvider = redissonClientProvider;
        this.userService = userService;
    }

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        if (rateLimit.rate() <= 0 || rateLimit.rateInterval() <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "限流配置错误");
        }
        String key = generateKey(point, rateLimit);
        Boolean acquired = tryAcquireDistributed(key, rateLimit);
        if (acquired == null) {
            acquired = localWindows.get(key, ignored -> new LocalWindow())
                    .tryAcquire(rateLimit.rate(), Duration.ofSeconds(rateLimit.rateInterval()).toMillis());
        }
        if (!acquired) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, rateLimit.message());
        }
        return point.proceed();
    }

    private Boolean tryAcquireDistributed(String key, RateLimit rateLimit) {
        try {
            RedissonClient client = redissonClientProvider.getIfAvailable();
            if (client == null) {
                return null;
            }
            RRateLimiter limiter = client.getRateLimiter(key);
            limiter.trySetRate(RateType.OVERALL, rateLimit.rate(), rateLimit.rateInterval(),
                    RateIntervalUnit.SECONDS);
            limiter.expire(Duration.ofSeconds(Math.max(60L, rateLimit.rateInterval() * 2L)));
            return limiter.tryAcquire();
        } catch (Exception e) {
            log.warn("Redis 限流不可用，使用本机限流兜底: {}", e.getMessage());
            return null;
        }
    }

    private String generateKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        StringBuilder key = new StringBuilder("rate_limit:");
        if (!rateLimit.key().isBlank()) {
            key.append(rateLimit.key()).append(':');
        }
        switch (rateLimit.limitType()) {
            case API -> {
                MethodSignature signature = (MethodSignature) point.getSignature();
                key.append("api:").append(signature.getDeclaringType().getSimpleName())
                        .append('.').append(signature.getName());
            }
            case USER -> key.append(resolveUserOrIp());
            case IP -> key.append("ip:").append(getClientIp());
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的限流类型");
        }
        return key.toString();
    }

    private String resolveUserOrIp() {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            try {
                User user = userService.getLoginUser(request);
                if (user != null && user.getId() != null) {
                    return "user:" + user.getId();
                }
            } catch (BusinessException ignored) {
                // 未登录请求继续按 IP 限流。
            }
        }
        return "ip:" + getClientIp();
    }

    private String getClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(',')).trim();
        }
        return ip == null || ip.isBlank() ? "unknown" : ip;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private static final class LocalWindow {
        private long startedAt;
        private int count;

        private synchronized boolean tryAcquire(int limit, long intervalMillis) {
            long now = System.currentTimeMillis();
            if (startedAt == 0 || now - startedAt >= intervalMillis) {
                startedAt = now;
                count = 0;
            }
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }
    }
}
