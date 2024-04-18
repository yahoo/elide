/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service.storageengine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.paiondata.elide.async.models.TableExport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.reactivex.plugins.RxJavaPlugins;
import redis.clients.jedis.JedisPooled;
import redis.embedded.RedisServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Test cases for RedisResultStorageEngine.
 */
public class RedisResultStorageEngineTest {
    private static final String HOST = "localhost";
    private static final int PORT = 6379;
    private static final int EXPIRATION_SECONDS = 120;
    private static final int BATCH_SIZE = 2;
    private JedisPooled jedisPool;
    private RedisServer redisServer;
    RedisResultStorageEngine engine;

    @BeforeEach
    public void setup() throws IOException {
        redisServer = new RedisServer(PORT);
        redisServer.start();
        jedisPool = new JedisPooled(HOST, PORT);
        engine = new RedisResultStorageEngine(jedisPool, EXPIRATION_SECONDS, BATCH_SIZE);
    }

    @AfterEach
    public void destroy() throws IOException {
        redisServer.stop();
    }

    public void write(OutputStream outputStream, String[] inputs) {
        boolean first = true;
        for (String input : inputs) {
            try {
                if (!first) {
                    outputStream.write('\n');
                }
                outputStream.write(input.getBytes(StandardCharsets.UTF_8));
                first = false;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Test
    public void testReadNonExistent() {
        assertThrows(IllegalStateException.class, () ->
                verifyResults("nonexisting_results", Arrays.asList(""))
        );
    }

    @Test
    public void testStoreEmptyResults() {
        String queryId = "store_empty_results_success";
        String validOutput = "";
        String[] input = validOutput.split("\n");

        storeResults(queryId, outputStream -> write(outputStream, input));

        // verify contents of stored files are readable and match original
        verifyResults("store_empty_results_success", Arrays.asList(validOutput));
    }

    @Test
    public void testStoreResults() {
        String queryId = "store_results_success";
        String validOutput = "hi\nhello";
        String[] input = validOutput.split("\n");

        storeResults(queryId, outputStream -> write(outputStream, input));

        // verify contents of stored files are readable and match original
        verifyResults("store_results_success", Arrays.asList(validOutput));
    }

    @Test
    public void testStoreBinaryResults() throws IOException {
        String queryId = "store_results_binary";
        byte[] data;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            for (int x = 0; x < 1000; x++) {
                outputStream.write("The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8));
            }
            data = outputStream.toByteArray();
        }
        storeResults(queryId, outputStream -> {
           try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
               zipOutputStream.setLevel(Deflater.NO_COMPRESSION);
               zipOutputStream.putNextEntry(new ZipEntry("test.txt"));
               zipOutputStream.write(data);
               zipOutputStream.closeEntry();
           } catch (IOException e) {
               throw new UncheckedIOException(e);
           }
        });

        byte[] results = readResultBytes(queryId);
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(results))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            assertEquals("test.txt", zipEntry.getName());
            byte[] result = zipInputStream.readAllBytes();
            assertArrayEquals(data, result);
        }
    }

    // Redis server does not exist.
    @Test
    public void testStoreResultsFail() throws IOException {
        io.reactivex.functions.Consumer<? super Throwable> old = RxJavaPlugins.getErrorHandler();

        try {
            RxJavaPlugins.setErrorHandler(e -> {
                // Ignore all errors
            });

            destroy();
            assertThrows(UncheckedIOException.class, () ->
                    storeResults("store_results_fail",
                            outputStream -> write(outputStream, new String[]{"hi", "hello"}))
            );
        } finally {
            RxJavaPlugins.setErrorHandler(old);
        }
    }

    @Test
    public void testReadResultsBatch() {
        String queryId = "store_results_batch_success";
        // 3 records > batchSize i.e 2
        String validOutput = "hi\nhello\nbye";
        String[] input = validOutput.split("\n");

        storeResults(queryId, outputStream -> write(outputStream, input));

        // 2 onnext calls will be returned.
        // 1st call will have 2 records combined together as one. hi and hello.
        // 2nd call will have 1 record only. bye.
        verifyResults("store_results_batch_success", Arrays.asList("hi\nhello", "bye"));
    }

    private void verifyResults(String queryId, List<String> expected) {
        String results = readResults(queryId);
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String input : expected) {
            if (!first) {
                builder.append('\n');
            }
            builder.append(input);
            first = false;
        }
        assertEquals(builder.toString(), results);
    }

    private String readResults(String queryId) {
        return new String(readResultBytes(queryId), StandardCharsets.UTF_8);
    }

    private byte[] readResultBytes(String queryId) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            engine.getResultsByID(queryId).accept(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void storeResults(String queryId, Consumer<OutputStream> storable) {
        TableExport query = new TableExport();
        query.setId(queryId);

        engine.storeResults(query.getId(), storable);
    }
}
