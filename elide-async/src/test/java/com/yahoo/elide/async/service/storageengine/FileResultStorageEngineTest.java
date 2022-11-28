/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service.storageengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.async.models.TableExport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.reactivex.Observable;

import java.io.File;
import java.nio.file.Path;

/**
 * Test cases for FileResultStorageEngine.
 */
public class FileResultStorageEngineTest {
    private static final String BASE_PATH = "src/test/resources/downloads/";

    @Test
    public void testRead() {
        String finalResult = readResultsFile(BASE_PATH, "non_empty_results");
        assertEquals(finalResult, "test");
    }

    @Test
    public void testReadEmptyFile() {
        String finalResult = readResultsFile(BASE_PATH, "empty_results");
        assertEquals(finalResult, "");
    }

    @Test
    public void testReadNonExistentFile() {
        assertThrows(IllegalStateException.class, () ->
                readResultsFile(BASE_PATH , "nonexisting_results")
        );
    }

    @Test
    public void testStoreResults(@TempDir Path tempDir) {
        String queryId = "store_results_success";
        String validOutput = "hi\nhello";
        String[] input = validOutput.split("\n");

        storeResultsFile(tempDir.toString(), queryId, Observable.fromArray(input));

        File file = new File(tempDir.toString() + File.separator + queryId);
        assertTrue(file.exists());

        // verify contents of stored files are readable and match original
        String finalResult = readResultsFile(tempDir.toString(), queryId);
        assertEquals(finalResult, validOutput);
    }

    // O/P Directory does not exist.
    @Test
    public void testStoreResultsFail(@TempDir File tempDir) {
        assertThrows(IllegalStateException.class, () ->
                storeResultsFile(tempDir.toString() + "invalid", "store_results_fail",
                        Observable.fromArray(new String[]{"hi", "hello"}))
        );
    }

    private String readResultsFile(String path, String queryId) {
        FileResultStorageEngine engine = new FileResultStorageEngine(path, false);

        return engine.getResultsByID(queryId).collect(() -> new StringBuilder(),
                (resultBuilder, tempResult) -> {
                    if (resultBuilder.length() > 0) {
                        resultBuilder.append(System.lineSeparator());
                    }
                    resultBuilder.append(tempResult);
                }
            ).map(StringBuilder::toString).blockingGet();
    }

    private void storeResultsFile(String path, String queryId, Observable<String> storable) {
        FileResultStorageEngine engine = new FileResultStorageEngine(path, false);
        TableExport query = new TableExport();
        query.setId(queryId);

        engine.storeResults(query, storable);
    }
}
