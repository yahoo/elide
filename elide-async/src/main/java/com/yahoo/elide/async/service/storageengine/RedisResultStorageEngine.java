/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service.storageengine;

import com.yahoo.elide.async.models.TableExport;
import io.reactivex.Observable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.inject.Singleton;

/**
 * Implementation of ResultStorageEngine that stores results on Redis Cluster.
 * It supports Async Module to store results with Table Export query.
 */
@Singleton
@Slf4j
@Getter
public class RedisResultStorageEngine implements ResultStorageEngine {
    @Setter private JedisPool jedisPool;
    @Setter private boolean enableExtension;
    @Setter private long expirationSeconds;

    /**
     * Constructor.
     * @param jedisPool Jedis Connection Pool to Redis clusteer.
     * @param enableExtension Enable file extensions.
     * @param expirationSeconds Expiration Time for results on Redis.
     */
    public RedisResultStorageEngine(JedisPool jedisPool, boolean enableExtension, long expirationSeconds) {
        this.jedisPool = jedisPool;
        this.enableExtension = enableExtension;
    }

    @Override
    public TableExport storeResults(TableExport tableExport, Observable<String> result) {
        log.debug("store TableExportResults for Download");

        try (Jedis jedis = jedisPool.getResource()) {
            result
                .map(record -> record.concat(System.lineSeparator()))
                .subscribe(
                        recordCharArray -> {
                            jedis.rpush(tableExport.getId(), recordCharArray);
                        },
                        throwable -> {
                            throw new IllegalStateException(STORE_ERROR, throwable);
                        }
                );
            jedis.expire(tableExport.getId(), expirationSeconds);
        } catch (Exception e) {
            throw new IllegalStateException(STORE_ERROR, e);
        }

        return tableExport;
    }

    @Override
    public Observable<String> getResultsByID(String tableExportID) {
        log.debug("getTableExportResultsByID");
        
        try (Jedis jedis = jedisPool.getResource()) {
            long recordCount = jedis.llen(tableExportID);

            if (recordCount == 0) {
            	throw new IllegalStateException(RETRIEVE_ERROR);
            } else {
                return Observable.fromIterable(() -> jedis.lrange(tableExportID, 0, -1).iterator());
            }
        }

        /* Alternative approach to go through list one by one.
        
        // Workaround for Local variable defined in an enclosing scope must be final or effectively final -> use Array.

        long[] recordRead = {0};
        long[] recordCount = {0};

        try (Jedis jedis = jedisPool.getResource()) {
            recordCount[0] = jedis.llen(tableExportID);
            if (recordCount[0] == 0) {
                throw new IllegalStateException(RETRIEVE_ERROR);
            }
        }

        return Observable.using(
                () -> jedisPool.getResource(),
                jedis -> Observable.fromIterable(() -> new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                            return recordRead[0] < recordCount[0];
                    }

                    @Override
                    public String next() {
                        long readCount = recordRead[0];
                        String record = jedis.lindex(tableExportID, readCount++);
                        recordRead[0] = readCount;
                        return record;
                    }
                }),
                Jedis::close);
        */
    }

    @Override
    public boolean isExtensionEnabled() {
        return this.enableExtension;
    }
}
