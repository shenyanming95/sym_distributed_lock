package com.sym.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * spring上下文工具类
 *
 * @author shenyanming
 * Created on 2020/8/13 17:14
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }

    public static <T> T getBean(Class<T> t) {
        if (applicationContext != null) {
            return applicationContext.getBean(t);
        }
        throw new NullPointerException("SpringUtil.applicationContext is null");
    }

    public static Object getBean(String name){
        if (applicationContext != null) {
            return applicationContext.getBean(name);
        }
        throw new NullPointerException("SpringUtil.applicationContext is null");
    }
}
