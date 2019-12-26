package com.sym.idempotency.service.impl;


import cn.hutool.crypto.SecureUtil;
import com.sym.idempotency.service.KeyGenerator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestPart;

import java.lang.reflect.Parameter;

/**
 * 默认生成幂等性分布式锁key的实现类, 默认会以方法的参数取md5加密串
 * <p>
 * Created by shenym on 2019/12/26.
 */
public class DefaultKeyGenerator implements KeyGenerator {

    @Override
    public String generate(ProceedingJoinPoint joinPoint) {
        StringBuilder sb = new StringBuilder();
        // 通过  方法签名 + 方法参数值 ==》 MD5加密后做 key
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        sb.append(methodSignature.toString());
        Parameter[] parameters = methodSignature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        // 排除那些大容量的参数值...
        int i = 0;
        for (Parameter parameter : parameters) {
            if (!parameter.isAnnotationPresent(RequestPart.class)) {
                sb.append(args[i]);
            }
            i++;
        }
        String result = sb.toString();
        String md5 = SecureUtil.md5(result);
        if (StringUtils.isEmpty(md5)) {
            return result;
        } else {
            return md5;
        }
    }

}
