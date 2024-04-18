/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.service.storageengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.async.models.TableExport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Test cases for FileResultStorageEngine.
 */
public class FileResultStorageEngineTest {
    private static final String BASE_PATH = "src/test/resources/downloads/";

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
    public void testRead() {
        String finalResult = readResultsFile(BASE_PATH, "non_empty_results");
        assertEquals("test\n", finalResult);
    }

    @Test
    public void testReadEmptyFile() {
        String finalResult = readResultsFile(BASE_PATH, "empty_results");
        assertEquals("", finalResult);
    }

    @Test
    public void testReadNonExistentFile() {
        assertThrows(UncheckedIOException.class, () ->
                readResultsFile(BASE_PATH , "nonexisting_results")
        );
    }

    @Test
    public void testReadPathTraversal() {
        assertThrows(UncheckedIOException.class, () ->
                readResultsFile(BASE_PATH , "../../../../../checkstyle-style.xml")
        );
    }

    @Test
    public void testStoreResults(@TempDir Path tempDir) {
        String queryId = "store_results_success";
        String validOutput = "hi\nhello";
        String[] input = validOutput.split("\n");

        storeResultsFile(tempDir.toString(), queryId, outputStream -> write(outputStream, input));

        File file = new File(tempDir.toString() + File.separator + queryId);
        assertTrue(file.exists());

        // verify contents of stored files are readable and match original
        String finalResult = readResultsFile(tempDir.toString(), queryId);
        assertEquals(finalResult, validOutput);
    }

    // O/P Directory does not exist.
    @Test
    public void testStoreResultsFail(@TempDir File tempDir) {
        assertThrows(UncheckedIOException.class, () ->
                storeResultsFile(tempDir.toString() + "invalid", "store_results_fail",
                       outputStream -> write(outputStream, new String[]{"hi", "hello"}))
        );
    }

    private String readResultsFile(String path, String queryId) {
        FileResultStorageEngine engine = new FileResultStorageEngine(path);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            engine.getResultsByID(queryId).accept(outputStream);
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void storeResultsFile(String path, String queryId, Consumer<OutputStream> storable) {
        FileResultStorageEngine engine = new FileResultStorageEngine(path);
        TableExport query = new TableExport();
        query.setId(queryId);

        engine.storeResults(query.getId(), storable);
    }
}
