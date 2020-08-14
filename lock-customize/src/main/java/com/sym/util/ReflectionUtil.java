package com.sym.util;

import com.sym.annotation.DistributedLock;
import com.sym.annotation.LockPrefixName;
import com.sym.enums.LockEnum;
import com.sym.exception.ReflectionException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 反射工具类
 *
 * @author shenyanming
 * Created on 2020/8/13 17:14
 */
public class ReflectionUtil {

    private static Comparator<Pair<LockPrefixName, Object>> comparator =
            (o1, o2) -> Integer.compare(o2.getLeft().position(), o1.getLeft().position());
    private static String delimiter = ":";

    /**
     * 通过{@link Method}获取到分布式锁的key
     *
     * @param method 被拦截的方法
     * @return 分布式锁key
     */
    public static String getLockKey(Method method, Object[] args) {
        if (Objects.isNull(method)) {
            throw new IllegalArgumentException("method is null");
        }
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        if (Objects.isNull(distributedLock)) {
            throw new ReflectionException(LockEnum.METHOD_WITHOUT_ANNOTATION_LOCK);
        }
        // 获取key前缀
        String prefix = distributedLock.prefix();
        // 获取key后缀
        String suffix = getKeySuffixFromParameters(method.getParameters(), args);
        return prefix.concat(suffix);
    }


    /**
     * 从方法参数中获取分布式锁的后缀
     *
     * @param parameters 参数集合
     * @return key后缀
     */
    private static String getKeySuffixFromParameters(Parameter[] parameters, Object[] args) {
        if (Objects.isNull(parameters) || parameters.length == 0) {
            return "";
        }
        List<Pair<LockPrefixName, Object>> list = new ArrayList<>(parameters.length);
        int index = 0;
        for (Parameter parameter : parameters) {
            LockPrefixName lockPrefixName = parameter.getAnnotation(LockPrefixName.class);
            if (lockPrefixName == null) {
                continue;
            }
            list.add(Pair.of(lockPrefixName, args[index++]));
        }
        // 执行排序
        list.sort(comparator);
        StringBuilder sb = new StringBuilder();
        list.stream().map(Pair::getRight).forEach(o -> sb.append(o).append(":"));
        return sb.substring(0, sb.length() - 1);
    }


    private static class Pair<L, R> {
        L left;
        R right;

        private Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public static <L, R> Pair<L, R> of(L left, R right) {
            return new Pair<>(left, right);
        }

        public L getLeft() {
            return left;
        }

        public R getRight() {
            return right;
        }
    }

}
