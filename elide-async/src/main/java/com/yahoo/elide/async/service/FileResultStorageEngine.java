/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncQuery;

import io.reactivex.Observable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import javax.inject.Singleton;

/**
 * Default implementation of ResultStorageEngine that stores results on local filesystem.
 * It supports Async Module to store results with async query.
 */
@Singleton
@Slf4j
@Getter
public class FileResultStorageEngine implements ResultStorageEngine {
    @Setter private String basePath;

    public FileResultStorageEngine() {
    }

    /**
     * Constructor.
     * @param basePath basePath for storing the files. Can be absolute or relative.
     */
    public FileResultStorageEngine(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public AsyncQuery storeResults(AsyncQuery asyncQuery, Observable<String> result) {
        log.debug("store AsyncResults for Download");

        try (BufferedWriter writer = getWriter(asyncQuery.getId())) {
            result
                .map(s -> s.concat(System.getProperty("line.separator")))
                .subscribe(
                        s -> {
                            writer.write(s);
                            writer.flush();
                        },
                        t -> {
                            throw new IllegalStateException(t);
                        },
                        () -> {
                            writer.flush();
                        }
                );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return asyncQuery;
    }

    @Override
    public Observable<String> getResultsByID(String asyncQueryID) {
        log.debug("getAsyncResultsByID");

        return Observable.using(
                () -> getReader(asyncQueryID),
                reader -> {
                    return Observable.fromIterable(() -> {
                        return new Iterator<String>() {
                            private String data = null;

                            @Override
                            public boolean hasNext() {
                                try {
                                    data = reader.readLine();
                                    return data != null;
                                } catch (IOException e) {
                                    throw new IllegalStateException(e);
                                }
                            }

                            @Override
                            public String next() {
                                if (data != null) {
                                    return data;
                                }
                                throw new IllegalStateException("null line found.");
                            }
                        };
                    });
                }, BufferedReader::close);
    }

    private BufferedReader getReader(String asyncQueryID) {
        try {
            return Files.newBufferedReader(Paths.get(basePath + File.separator + asyncQueryID));
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new IllegalStateException("Unable to retrieve results.");
        }
    }

    private BufferedWriter getWriter(String asyncQueryID) {
        try {
            return Files.newBufferedWriter(Paths.get(basePath + File.separator + asyncQueryID));
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new IllegalStateException("Unable to store results.");
        }
    }
}
