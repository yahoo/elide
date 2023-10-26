/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service.storageengine;

import com.yahoo.elide.async.ResultTypeFileExtensionMapper;
import com.yahoo.elide.async.io.ByteSinkOutputStream;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;

import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.UnifiedJedis;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Implementation of ResultStorageEngine that stores results on Redis Cluster.
 * It supports Async Module to store results with Table Export query.
 */
@Singleton
@Slf4j
@Getter
public class RedisResultStorageEngine implements ResultStorageEngine {
    @Setter
    private UnifiedJedis jedis;
    @Setter
    private ResultTypeFileExtensionMapper resultTypeFileExtensionMapper;
    @Setter
    private long expirationSeconds;
    @Setter
    private long batchSize;

    /**
     * Constructor.
     *
     * @param jedis                         Jedis Connection Pool to Redis clusteer.
     * @param resultTypeFileExtensionMapper Enable file extensions.
     * @param expirationSeconds             Expiration Time for results on Redis.
     * @param batchSize                     Batch Size for retrieving from Redis.
     */
    public RedisResultStorageEngine(UnifiedJedis jedis, ResultTypeFileExtensionMapper resultTypeFileExtensionMapper,
            long expirationSeconds, long batchSize) {
        this.jedis = jedis;
        this.resultTypeFileExtensionMapper = resultTypeFileExtensionMapper;
        this.expirationSeconds = expirationSeconds;
        this.batchSize = batchSize;
    }

    @Override
    public TableExportResult storeResults(TableExport tableExport, Consumer<OutputStream> result) {
        log.debug("store TableExportResults for Download");
        String extension = resultTypeFileExtensionMapper != null
                ? resultTypeFileExtensionMapper.getFileExtension(tableExport.getResultType())
                : "";

        TableExportResult exportResult = new TableExportResult();
        String key = tableExport.getId() + extension;
        ByteSinkOutputStream byteSinkOutputStream = new ByteSinkOutputStream(data -> {
            jedis.rpush(key.getBytes(StandardCharsets.UTF_8), data);
        });
        try {
            result.accept(byteSinkOutputStream);
        } catch (RuntimeException e) {
            StringBuilder message = new StringBuilder();
            message.append(e.getClass().getCanonicalName()).append(" : ");
            message.append(e.getMessage());
            exportResult.setMessage(message.toString());

            throw new IllegalStateException(STORE_ERROR, e);
        }
        jedis.expire(key, expirationSeconds);
        return exportResult;
    }

    @Override
    public Consumer<OutputStream> getResultsByID(String tableExportID) {
        log.debug("getTableExportResultsByID");

        long recordCount = jedis.llen(tableExportID);

        if (recordCount == 0) {
            throw new IllegalStateException(RETRIEVE_ERROR);
        } else {
            return outputStream -> {
                long recordRead = 0;
                while (recordRead < recordCount) {
                    long end = recordRead + batchSize - 1; // index of last element.

                    if (end >= recordCount) {
                        end = recordCount - 1;
                    }

                    for (byte[] data : jedis.lrange(tableExportID.getBytes(StandardCharsets.UTF_8), recordRead, end)) {
                        try {
                            outputStream.write(data);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    recordRead = end + 1; //index for next element to be read
                }
            };
        }
    }

    @Override
    public ResultTypeFileExtensionMapper getResultTypeFileExtensionMapper() {
        return this.resultTypeFileExtensionMapper;
    }
}
