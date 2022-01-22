/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service.storageengine;

import com.yahoo.elide.async.models.FileExtensionType;
import com.yahoo.elide.async.models.TableExport;

import io.reactivex.Observable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Iterator;

import javax.inject.Singleton;

/**
 * Implementation of ResultStorageEngine that stores results on Redis Cluster.
 * It supports Async Module to store results with Table Export query.
 */
@Singleton
@Slf4j
@Getter
public class RedisResultStorageEngine implements ResultStorageEngine {
    private static final int BATCH_SIZE = 500;
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
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public TableExport storeResults(TableExport tableExport, Observable<String> result) {
        log.debug("store TableExportResults for Download");
        String extension = this.isExtensionEnabled()
                ? tableExport.getResultType().getFileExtensionType().getExtension()
                : FileExtensionType.NONE.getExtension();

        String key = tableExport.getId() + extension;
        try (Jedis jedis = jedisPool.getResource()) {
            result
                .map(record -> record)
                .subscribe(
                        recordCharArray -> {
                            long i = jedis.rpush(key, recordCharArray);
                        },
                        throwable -> {
                            throw new IllegalStateException(STORE_ERROR, throwable);
                        }
                );
            jedis.expire(key, expirationSeconds);
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
                // Workaround for Local variable defined in an enclosing scope must be final or effectively final;
                // use Array.
                long[] recordRead = {0};
                return Observable.fromIterable(() -> new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                            return recordRead[0] < recordCount;
                    }
                    @Override
                    public String next() {
                        StringBuilder record = new StringBuilder();
                        long end = recordRead[0] + BATCH_SIZE - 1;

                        if (end >= recordCount) {
                            end = recordCount - 1;
                        }

                        Iterator<String> itr = jedis.lrange(tableExportID, recordRead[0], end).iterator();

                        while (itr.hasNext()) {
                            String str = itr.next();
                            record.append(str).append(System.lineSeparator());
                        }
                        recordRead[0] = recordRead[0] + BATCH_SIZE;

                        if (recordRead[0] > recordCount) {
                            return record.substring(0, record.length() - 1);
                        }
                        return record.toString();
                    }
                });
            }
        }
    }

    @Override
    public boolean isExtensionEnabled() {
        return this.enableExtension;
    }
}
