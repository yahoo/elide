/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.async.models.AsyncQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.reactivex.Observable;

import java.io.File;
import java.nio.file.Path;

/**
 * Test cases for FileResultStorageEngine.
 */
public class FileResultStorageEngineTest {

    @Test
    public void testRead() {
        FileResultStorageEngine engine = new FileResultStorageEngine("src/test/resources");
        String queryId = "bb31ca4e-ed8f-4be0-a0f3-12099fb9263f";
        String finalResult = engine.getResultsByID(queryId).collect(() -> new StringBuilder(),
                (resultBuilder, tempResult) -> {
                    if (resultBuilder.length() > 0) {
                        resultBuilder.append(System.getProperty("line.separator"));
                    }
                    resultBuilder.append(tempResult);
                }
                ).map(StringBuilder::toString).blockingGet();

        assertEquals(finalResult, "test");
    }

    @Test
    public void testReadEmptyFile() {
        FileResultStorageEngine engine = new FileResultStorageEngine("src/test/resources");
        String finalResult = engine.getResultsByID("bb31ca4e-ed8f-4be0-a0f3-12099fb9263e").collect(() -> new StringBuilder(),
                (resultBuilder, tempResult) -> {
                    if (resultBuilder.length() > 0) {
                        resultBuilder.append(System.getProperty("line.separator"));
                    }
                    resultBuilder.append(tempResult);
                }
                ).map(StringBuilder::toString).blockingGet();

        assertEquals(finalResult, "");
    }

    @Test
    public void testReadNonExistentFile() {
        FileResultStorageEngine engine = new FileResultStorageEngine("src/test/resources");
        assertThrows(IllegalStateException.class, () ->
            engine.getResultsByID("bb31ca4e-ed8f-4be0-a0f3-12099fb9263d").collect(() -> new StringBuilder(),
                (resultBuilder, tempResult) -> {
                    if (resultBuilder.length() > 0) {
                        resultBuilder.append(System.getProperty("line.separator"));
                    }
                    resultBuilder.append(tempResult);
                }
            ).map(StringBuilder::toString).blockingGet()
        );
    }

    @Test
    public void testStoreResults(@TempDir Path tempDir) {
        String queryId = "bb31ca4e-ed8f-4be0-a0f3-12099fb9263c";
        FileResultStorageEngine engine = new FileResultStorageEngine(tempDir.toString());
        AsyncQuery query = new AsyncQuery();
        query.setId(queryId);
        engine.storeResults(query, Observable.fromArray(new String[]{"hi", "hello"}));
        File file = new File(tempDir.toString() + File.separator + queryId);
        assertTrue(file.exists());
    }

    @Test
    public void testStoreResultsFail(@TempDir File tempDir) {
        String queryId = "bb31ca4e-ed8f-4be0-a0f3-12099fb9263b";
        FileResultStorageEngine engine = new FileResultStorageEngine(tempDir.toString() + "invalid");
        AsyncQuery query = new AsyncQuery();
        query.setId(queryId);
        assertThrows(IllegalStateException.class, () ->
            engine.storeResults(query, Observable.fromArray(new String[]{"hi", "hello"})));
    }
}
