package com.demo.service;

import com.demo.model.User;
import com.demo.config.RedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USER_KEY_PREFIX = "user:";

    public void saveUserToRedis(User user) {
        try (Jedis jedis = RedisConfig.getJedisConnection()) {
            String userJson = objectMapper.writeValueAsString(user);
            String key = USER_KEY_PREFIX + user.getId();
            jedis.set(key, userJson);
            logger.info("User saved to Redis with key: {}", key);
        } catch (Exception e) {
            logger.error("Error saving user to Redis", e);
        }
    }

    public List<User> getUsersFromRedis(int start, int size) {
        List<User> users = new ArrayList<>();
        try (Jedis jedis = RedisConfig.getJedisConnection()) {
            // Get all keys matching the pattern
            Set<String> keys = jedis.keys(USER_KEY_PREFIX + "*");
            logger.info("Found {} keys in Redis", keys.size());

            // Convert to list and apply pagination
            List<String> keysList = new ArrayList<>(keys);
            int end = Math.min(start + size, keysList.size());

            for (int i = start; i < end; i++) {
                String key = keysList.get(i);
                String userJson = jedis.get(key);
                if (userJson != null) {
                    User user = objectMapper.readValue(userJson, User.class);
                    users.add(user);
                }
            }
            logger.info("Retrieved {} users from Redis", users.size());
        } catch (Exception e) {
            logger.error("Error retrieving users from Redis", e);
        }
        return users;
    }

    public User getUserFromRedis(Long id) {
        try (Jedis jedis = RedisConfig.getJedisConnection()) {
            String key = USER_KEY_PREFIX + id;
            String userJson = jedis.get(key);

            if (userJson != null) {
                User user = objectMapper.readValue(userJson, User.class);
                logger.debug("Retrieved user from Redis with id: {}", id);
                return user;
            } else {
                logger.debug("No user found in Redis with id: {}", id);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error retrieving user from Redis with id: {}", id, e);
            return null;
        }
    }

    public int getTotalCountFromRedis() {
        try (Jedis jedis = RedisConfig.getJedisConnection()) {
            Set<String> keys = jedis.keys(USER_KEY_PREFIX + "*");
            return keys.size();
        } catch (Exception e) {
            logger.error("Error getting total count from Redis", e);
            return 0;
        }
    }

    // Utility method to sync DB data to Redis
    public void syncDatabaseToRedis(List<User> users) {
        try (Jedis jedis = RedisConfig.getJedisConnection()) {
            for (User user : users) {
                String userJson = objectMapper.writeValueAsString(user);
                String key = USER_KEY_PREFIX + user.getId();
                jedis.set(key, userJson);
            }
            logger.info("Synced {} users to Redis", users.size());
        } catch (Exception e) {
            logger.error("Error syncing database to Redis", e);
        }
    }
}