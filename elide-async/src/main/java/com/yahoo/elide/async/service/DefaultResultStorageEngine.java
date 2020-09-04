/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;

import io.reactivex.Observable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

/**
 * Default implementation of ResultStorageEngine.
 * It supports Async Module to store results with async query.
 */
@Singleton
@Slf4j
@Getter
public class DefaultResultStorageEngine implements ResultStorageEngine {
    private static final String NEW_LINE_REGEX = "\\r?\\n";

    @Setter private ElideSettings elideSettings;
    @Setter private AsyncQueryDAO defaultAsyncQueryDAO;

    public DefaultResultStorageEngine() {
    }

    /**
     * Constructor.
     * @param elideSettings ElideSettings object
     * @param defaultAsyncQueryDAO AsyncQueryDAO Object
     */
    public DefaultResultStorageEngine(ElideSettings elideSettings, AsyncQueryDAO defaultAsyncQueryDAO) {
        this.elideSettings = elideSettings;
        this.defaultAsyncQueryDAO = defaultAsyncQueryDAO;
    }

    @Override
    public AsyncQuery storeResults(AsyncQuery asyncQuery, Observable<String> result) {
        log.debug("store AsyncResults for Download");

        String finalResult = result.collect(() -> new StringBuilder(),
                (resultBuilder, tempResult) -> {
                    if (resultBuilder.length() > 0) {
                        resultBuilder.append(System.getProperty("line.separator"));
                    }
                    resultBuilder.append(tempResult);
                }
            ).map(StringBuilder::toString).blockingGet();

        asyncQuery.getResult().setAttachment(finalResult);
        return asyncQuery;
    }

    @Override
    public Observable<String> getResultsByID(String asyncQueryID) {
        log.debug("getAsyncResultsByID");

        Optional<AsyncQuery> asyncQuery = null;
        Observable<String> observableResult = Observable.empty();

        PathElement idPathElement = new PathElement(AsyncQuery.class, String.class, "id");
        List<String> idList =  Collections.singletonList(asyncQueryID);
        FilterExpression fltStatusExpression =
                new InPredicate(idPathElement, idList);

        Collection<AsyncQuery> asyncQueryCollection =
                defaultAsyncQueryDAO.loadAsyncQueryCollection(fltStatusExpression);

        if (asyncQueryCollection != null && asyncQueryCollection.size() > 0) {
            asyncQuery = asyncQueryCollection.stream().findAny();
            AsyncQueryResult queryResult = asyncQuery.get().getResult();
            if (queryResult != null && queryResult.getAttachment() != null) {
                observableResult = Observable.fromArray(queryResult.getAttachment().split(NEW_LINE_REGEX));
            }
        }

        return observableResult;
    }
}
