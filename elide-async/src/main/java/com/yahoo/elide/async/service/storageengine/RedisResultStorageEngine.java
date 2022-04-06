/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service.storageengine;

import com.yahoo.elide.async.models.FileExtensionType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;
import io.reactivex.Observable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.UnifiedJedis;

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
    @Setter private UnifiedJedis jedis;
    @Setter private boolean enableExtension;
    @Setter private long expirationSeconds;
    @Setter private long batchSize;

    /**
     * Constructor.
     * @param jedis Jedis Connection Pool to Redis clusteer.
     * @param enableExtension Enable file extensions.
     * @param expirationSeconds Expiration Time for results on Redis.
     * @param batchSize Batch Size for retrieving from Redis.
     */
    public RedisResultStorageEngine(UnifiedJedis jedis, boolean enableExtension, long expirationSeconds,
            long batchSize) {
        this.jedis = jedis;
        this.enableExtension = enableExtension;
        this.expirationSeconds = expirationSeconds;
        this.batchSize = batchSize;
    }

    @Override
    public TableExportResult storeResults(TableExport tableExport, Observable<String> result) {
        log.debug("store TableExportResults for Download");
        String extension = this.isExtensionEnabled()
                ? tableExport.getResultType().getFileExtensionType().getExtension()
                : FileExtensionType.NONE.getExtension();

        TableExportResult exportResult = new TableExportResult();
        String key = tableExport.getId() + extension;

        result
            .map(record -> record)
            .subscribe(
                    recordCharArray -> {
                        jedis.rpush(key, recordCharArray);
                    },
                    throwable -> {
                        StringBuilder message = new StringBuilder();
                        message.append(throwable.getClass().getCanonicalName()).append(" : ");
                        message.append(throwable.getMessage());
                        exportResult.setMessage(message.toString());

                        throw new IllegalStateException(STORE_ERROR, throwable);
                    }
            );
        jedis.expire(key, expirationSeconds);

        return exportResult;
    }

    @Override
    public Observable<String> getResultsByID(String tableExportID) {
        log.debug("getTableExportResultsByID");

        long recordCount = jedis.llen(tableExportID);

        if (recordCount == 0) {
            throw new IllegalStateException(RETRIEVE_ERROR);
        } else {
            // Workaround for Local variable defined in an enclosing scope must be final or effectively final;
            // use Array.
            long[] recordRead = {0}; // index to start.
            return Observable.fromIterable(() -> new Iterator<String>() {
                @Override
                public boolean hasNext() {
                        return recordRead[0] < recordCount;
                }
                @Override
                public String next() {
                    StringBuilder record = new StringBuilder();
                    long end = recordRead[0] + batchSize - 1; // index of last element.

                    if (end >= recordCount) {
                        end = recordCount - 1;
                    }

                    Iterator<String> itr = jedis.lrange(tableExportID, recordRead[0], end).iterator();

                    // Combine the list into a single string.
                    while (itr.hasNext()) {
                        String str = itr.next();
                        record.append(str).append(System.lineSeparator());
                    }
                    recordRead[0] = end + 1; //index for next element to be read

                    // Removing the last line separator because ExportEndPoint will add 1 more.
                    return record.substring(0, record.length() - System.lineSeparator().length());
                }
            });
        }
    }

    @Override
    public boolean isExtensionEnabled() {
        return this.enableExtension;
    }
}
