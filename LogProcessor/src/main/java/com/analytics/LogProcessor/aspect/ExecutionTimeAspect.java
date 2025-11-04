package com.analytics.LogProcessor.aspect;

import com.analytics.LogProcessor.annotation.TrackExecutionTime;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    @Around("@annotation(trackExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint,
                                   TrackExecutionTime trackExecutionTime) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long duration = System.currentTimeMillis() - start;
        String methodName = joinPoint.getSignature().toShortString();
        String label = trackExecutionTime.value().isEmpty() ? methodName : trackExecutionTime.value();

        log.info("[Timing] {} took {} ms to send records to analytics service", label, duration);

        return result;
    }
}
