package com.zw;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * spring redis 工具类
 *
 * @author weijianbo
 **/
@SuppressWarnings(value = {"unchecked", "rawtypes"})
@Component
public class RedisService {
    @Autowired
    public RedisTemplate redisTemplate;

    // Lua脚本：原子获取并删除字符串值
    private static final String GET_DEL_SCRIPT =
            "local value = redis.call('GET', KEYS[1])\n" +
                    "if value then\n" +
                    "    redis.call('DEL', KEYS[1])\n" +
                    "end\n" +
                    "return value";

    private DefaultRedisScript<String> getDelScript;

    @PostConstruct
    public void init() {
        // 初始化Lua脚本
        getDelScript = new DefaultRedisScript<>();
        getDelScript.setScriptText(GET_DEL_SCRIPT);
        getDelScript.setResultType(String.class);
    }

    /**
     * 原子性地获取并删除字符串值（Lua脚本实现）
     * 这个方法保证获取值和删除key的原子性
     *
     * @param key Redis键
     * @return 获取到的值，如果key不存在则返回null
     */
    public String getAndDelete(final String key) {
        try {
            return (String) redisTemplate.execute(
                    getDelScript,
                    Collections.singletonList(key)
            );
        } catch (Exception e) {
            // 如果脚本执行失败，降级处理（非原子性）
            String value = getCacheObject(key);
            if (value != null) {
                deleteObject(key);
            }
            return value;
        }
    }


    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public Boolean setIfAbsent(final String key, final String value, final Long timeout, final TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(key,value,timeout,timeUnit);
    }

    /**
     * 向有序集合添加元素
     * @param key Redis键
     * @param value 值
     * @param score 分数（排序依据）
     * @return 是否成功
     */
    public <T> Boolean addZSet(final String key, final T value, final double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 从有序集合中删除指定元素
     * @param key Redis键
     * @param values 要删除的值
     * @return 成功删除的数量
     */
    public Long removeZSet(final String key, final String values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final Collection collection) {
        return redisTemplate.delete(collection) > 0;
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet) {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext()) {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 获得缓存的zset
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheZSet(final String key) {
        return redisTemplate.opsForZSet().range(key, 0, -1);
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys) {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey) {
        return redisTemplate.opsForHash().delete(key, hKey) > 0;
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }


    public void batchUpdateMsgStatus(final List<String> msgIdList, final Integer status) {
        // 将msgIdList中的msgId进行批量更新
        Map<String, Integer> idsMap = new HashMap<>();

        for (String msgId : msgIdList) {
            idsMap.put(msgId, status);
        }
        redisTemplate.opsForValue().multiSet(idsMap);
    }

    /**
     * 向 Set 中添加单个元素
     *
     * @param key Redis键
     * @param value 值
     * @return 添加数量
     */
    public <T> Long addCacheSetValue(final String key, final T value) {
        return redisTemplate.opsForSet().add(key, value);
    }

    /**
     * 删除 Set 中的指定元素
     *
     * @param key Redis键
     * @param value 值
     * @return 删除数量
     */
    public Long removeCacheSetValue(final String key, final Object value) {
        return redisTemplate.opsForSet().remove(key, value);
    }


}

