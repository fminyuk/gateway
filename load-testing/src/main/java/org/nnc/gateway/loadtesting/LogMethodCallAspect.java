package org.nnc.gateway.loadtesting;

import org.apache.commons.lang3.builder.ToStringStyle;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

@Aspect
@Component
public class LogMethodCallAspect {
    private static final Logger LOG = LoggerFactory.getLogger(LogMethodCallAspect.class);

    @Around("@annotation(LogMethodCall)")
    public Object logMethodCall(ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        final String methodName = joinPoint.getSignature().getName();

        final String[] methodArgNames = signature.getParameterNames();
        final Object[] methodArgValues = joinPoint.getArgs();
        assert methodArgValues.length == methodArgNames.length;

        final String methodArgs = Stream.iterate(0, i -> i + 1).
                limit(methodArgNames.length).
                map(i -> {
                    final String name = methodArgNames[i];
                    final String value = reflectionToString(methodArgValues[i], ToStringStyle.JSON_STYLE);
                    return name + "=" + value;
                }).
                collect(Collectors.joining(","));

        LOG.info("method call: " + methodName + "(" + methodArgs + ")");

        return joinPoint.proceed();
    }
}
