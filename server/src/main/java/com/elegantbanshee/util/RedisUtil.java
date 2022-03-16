package com.elegantbanshee.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReentrantLock;

public class RedisUtil {
    private static JedisPool pool;
    private static final ReentrantLock LOCK = new ReentrantLock();

    public static void storeCommand(String code, String command) {
        LOCK.lock();
        try (Jedis jedis = pool.getResource()) {
            jedis.set(code, command);
            jedis.expire(code, 5);
        }
        LOCK.unlock();
    }

    @Nullable
    public static String getCommand(String code) {
        LOCK.lock();
        String command;
        try (Jedis jedis = pool.getResource()) {
            command = jedis.get(code);
            jedis.del(code);
        }
        LOCK.unlock();
        return command;
    }

    public static void start(String redisUrl) {
        pool = new JedisPool(redisUrl);
    }
}
