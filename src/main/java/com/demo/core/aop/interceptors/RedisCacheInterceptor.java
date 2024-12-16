package com.demo.core.aop.interceptors;

import com.demo.config.RedisConfig;
import com.demo.core.annotation.RedisCache;
import com.demo.utils.SerializationUtil;
import redis.clients.jedis.Jedis;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.reflect.Method;

@Interceptor
@RedisCache
public class RedisCacheInterceptor implements Serializable {
    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object cache(InvocationContext context) throws Exception {

        Method method = context.getMethod();
        RedisCache cacheAnnotation = method.getAnnotation(RedisCache.class);

        if (cacheAnnotation == null || !cacheAnnotation.condition()) {
            return context.proceed();
        }

        String cacheKey = generateCacheKey(context, cacheAnnotation);

        try (Jedis jedis = RedisConfig.getJedisPool().getResource()) {
            // Check data from cache
            String cachedValue = jedis.get(cacheKey);

            if (cachedValue != null) {
                return SerializationUtil.deserialize(cachedValue, method.getReturnType());
            }

            // If not in cache, run the method
            Object result = context.proceed();

            if (result != null) {
                String serializedValue = SerializationUtil.serialize(result);
                jedis.setex(cacheKey, cacheAnnotation.timeout(), serializedValue);
            }

            return result;

        } catch (Exception e) {
            // In case of Redis error, run the normal method
            return context.proceed();
        }
    }

    private String generateCacheKey(InvocationContext context, RedisCache cache) {
        if (!cache.key().isEmpty()) {
            String key = cache.key();
            Object[] parameters = context.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                key = key.replace("{" + i + "}", String.valueOf(parameters[i]));
            }
            return key;
        }

        StringBuilder key = new StringBuilder();
        key.append(context.getTarget().getClass().getName())
                .append(".")
                .append(context.getMethod().getName());

        for (Object param : context.getParameters()) {
            key.append(".").append(param != null ? param.toString() : "null");
        }

        return key.toString();
    }
}
