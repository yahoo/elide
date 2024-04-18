/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.cache;

import com.paiondata.elide.datastores.aggregation.query.QueryResult;
import org.springframework.util.SerializationUtils;

import lombok.Setter;
import redis.clients.jedis.UnifiedJedis;

/**
 * A Redis cache.
 */
public class RedisCache implements Cache {
    @Setter private UnifiedJedis jedis;
    @Setter private long defaultExpirationMinutes;

    /**
     * Constructor.
     * @param jedis Jedis Connection Pool to Redis clusteer.
     * @param defaultExpirationMinutes Expiration Time for results on Redis.
     */
    public RedisCache(UnifiedJedis jedis, long defaultExpirationMinutes) {
        this.jedis = jedis;
        this.defaultExpirationMinutes = defaultExpirationMinutes;
    }

    @Override
    public QueryResult get(Object key) {
        return (QueryResult) SerializationUtils.deserialize(jedis.get(SerializationUtils.serialize(key)));
    }

    @Override
    public void put(Object key, QueryResult result) {
        byte[] keyBytes = SerializationUtils.serialize(key);
        jedis.set(keyBytes, SerializationUtils.serialize(result));
        jedis.expire(keyBytes, defaultExpirationMinutes * 60);
    }
}
