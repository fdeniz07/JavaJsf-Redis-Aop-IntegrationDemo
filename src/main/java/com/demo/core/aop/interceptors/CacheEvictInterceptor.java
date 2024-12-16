package com.demo.core.aop.interceptors;


import com.demo.config.RedisConfig;
import com.demo.core.annotation.CacheEvict;
import redis.clients.jedis.Jedis;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.reflect.Method;

@Interceptor
@CacheEvict
public class CacheEvictInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object evictCache(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        CacheEvict evictAnnotation = method.getAnnotation(CacheEvict.class);

        if (evictAnnotation != null) {
            try (Jedis jedis = RedisConfig.getJedisPool().getResource()) {
                if (evictAnnotation.allEntries()) {
                    // Delete all keys that match the pattern
                    String pattern = evictAnnotation.key() + "*";
                    for (String key : jedis.keys(pattern)) {
                        jedis.del(key);
                    }
                } else {
                    String key = evictAnnotation.key();
                    Object[] parameters = context.getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        key = key.replace("{" + i + "}", String.valueOf(parameters[i]));
                    }
                    jedis.del(key);
                }
            }
        }

        return context.proceed();
    }
}
