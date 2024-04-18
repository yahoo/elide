/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.async.service.storageengine;

import com.paiondata.elide.async.io.ByteSinkOutputStream;
import com.paiondata.elide.async.models.TableExportResult;

import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.UnifiedJedis;

import java.io.BufferedOutputStream;
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
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    @Setter
    private UnifiedJedis jedis;
    @Setter
    private long expirationSeconds;
    @Setter
    private long batchSize;
    @Setter
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    /**
     * Constructor.
     *
     * @param jedis                         Jedis Connection Pool to Redis clusteer.
     * @param expirationSeconds             Expiration Time for results on Redis.
     * @param batchSize                     Batch Size for retrieving from Redis.
     */
    public RedisResultStorageEngine(UnifiedJedis jedis, long expirationSeconds, long batchSize) {
        this.jedis = jedis;
        this.expirationSeconds = expirationSeconds;
        this.batchSize = batchSize;
    }

    @Override
    public TableExportResult storeResults(String tableExportID, Consumer<OutputStream> result) {
        log.debug("store TableExportResults for Download");
        TableExportResult exportResult = new TableExportResult();
        String key = tableExportID;
        try (OutputStream outputStream = newBufferedOutputStream(key, bufferSize)) {
            result.accept(outputStream);
        } catch (IOException e) {
            setMessage(exportResult, e);
            throw new UncheckedIOException(STORE_ERROR, e);
        } catch (UncheckedIOException e) {
            setMessage(exportResult, e);
            throw e;
        } catch (RuntimeException e) {
            setMessage(exportResult, e);
            throw new UncheckedIOException(STORE_ERROR, new IOException(e));
        }
        jedis.expire(key, expirationSeconds);
        return exportResult;
    }

    protected void setMessage(TableExportResult exportResult, Exception e) {
        StringBuilder message = new StringBuilder();
        message.append(e.getClass().getCanonicalName()).append(" : ");
        message.append(e.getMessage());
        exportResult.setMessage(message.toString());
    }

    protected OutputStream newBufferedOutputStream(String key, int size) {
        return new BufferedOutputStream(newOutputStream(key), size);
    }

    protected OutputStream newOutputStream(String key) {
        return new RedisOutputStream(data -> {
            jedis.rpush(key.getBytes(StandardCharsets.UTF_8), data);
        }, () -> jedis.rpush(key.getBytes(StandardCharsets.UTF_8), new byte[] {}));
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

    public static class RedisOutputStream extends ByteSinkOutputStream {
        private final Runnable onEmpty;
        private boolean empty = true;

        public RedisOutputStream(Consumer<byte[]> byteSink, Runnable onEmpty) {
            super(byteSink);
            this.onEmpty = onEmpty;
        }

        @Override
        public void write(int b) throws IOException {
            this.empty = false;
            super.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            this.empty = false;
            super.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.empty = false;
            super.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (this.empty && onEmpty != null) {
                onEmpty.run();
            }
        }
    }
}
