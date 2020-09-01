/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.AsyncQueryResult;
import com.yahoo.elide.core.DataStore;
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

    @Setter private ElideSettings elideSettings;
    @Setter private DataStore dataStore;
    @Setter private AsyncQueryDAO defaultAsyncQueryDAO;

    public DefaultResultStorageEngine() {
    }

    /**
     * Constructor.
     * @param elideSettings ElideSettings object
     * @param dataStore DataStore Object
     */
    public DefaultResultStorageEngine(ElideSettings elideSettings, DataStore dataStore,
            AsyncQueryDAO defaultAsyncQueryDAO) {
        this.elideSettings = elideSettings;
        this.dataStore = dataStore;
        this.defaultAsyncQueryDAO = defaultAsyncQueryDAO;
    }

    @Override
    public AsyncQueryResult storeResults(AsyncQueryResult asyncQueryResult, String result, String asyncQueryId) {
        log.debug("store AsyncResults for Download");

        asyncQueryResult.setAttachment(result);

        return asyncQueryResult;
    }

    @Override
    public Observable<String> getResultsByID(String asyncQueryID) {
        log.debug("getAsyncResultsByID");

        Optional<AsyncQuery> asyncQuery = null;
        String result = null;
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
                result = queryResult.getAttachment();
                observableResult = Observable.just(result);
            }
        }

        return observableResult;
    }
}
