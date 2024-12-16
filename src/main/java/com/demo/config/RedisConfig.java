package com.demo.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);
    private static JedisPool jedisPool;

    static {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(128);
            poolConfig.setMaxIdle(128);
            poolConfig.setMinIdle(16);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);

            String host = AppConfig.getProperty("redis.host", "localhost");
            int port = Integer.parseInt(AppConfig.getProperty("redis.port", "6379"));
            int timeout = Integer.parseInt(AppConfig.getProperty("redis.timeout", "2000"));
            String password = AppConfig.getProperty("redis.password");
            int database = Integer.parseInt(AppConfig.getProperty("redis.database", "0"));

            if (password != null && !password.trim().isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, timeout, password, database);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, timeout);
            }

            logger.info("Redis connection pool initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize Redis pool", e);
            throw new RuntimeException("Failed to initialize Redis pool", e);
        }
    }

    public static Jedis getJedisConnection() {
        try {
            return jedisPool.getResource();
        } catch (Exception e) {
            logger.error("Failed to get Redis connection", e);
            throw e;
        }
    }

    public static JedisPool getJedisPool() {
        return jedisPool;
    }
}