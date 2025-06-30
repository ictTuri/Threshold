package al.lhind.tab.claim.aspect;

import al.lhind.tab.claim.aspect.annotations.RateLimit;
import al.lhind.tab.claim.util.RateDataUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateDataUtil rateDataUtil;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Around("@annotation(al.lhind.tab.claim.aspect.annotations.RateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ip = request.getRemoteAddr();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String key = ip + ":" + method.getName();

        int capacity = rateDataUtil.getCapacity();
        int tokens = rateDataUtil.getTokens();
        int minutes = rateDataUtil.getMinutes();

        Bucket bucket = buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(
                    capacity,
                    Refill.greedy(tokens, Duration.ofMinutes(minutes))
            );
            return Bucket.builder().addLimit(limit).build();
        });

        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        } else {
            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
            if (response != null) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too Many Requests");
                response.getWriter().flush();
            }
            return null;
        }
    }

    public void clearBuckets() {
        buckets.clear();
        log.info("Rate limit buckets cleared / For testing purposes only");
    }
}
