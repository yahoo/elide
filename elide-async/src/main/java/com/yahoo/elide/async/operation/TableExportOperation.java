/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.operation;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.User;

import io.reactivex.Observable;

/**
 * TableExport Execute Operation Interface.
 */
public abstract class TableExportOperation implements AsyncAPIOperation<TableExport> {

    /**
     * Export Table Data.
     * @param query TableExport type object.
     * @param user User object.
     * @param apiVersion API Version.
     * @return Observable PersistentResource
     */
    public Observable<PersistentResource> export(TableExport query, User user, String apiVersion) {
        // TODO Add logic Here
        return Observable.empty();
    }

    /**
     * Initialize and get RequestScope.
     * @param query TableExport type object.
     * @param elide Elide Instance.
     * @param user User Object.
     * @param apiVersion API Version.
     * @return RequestScope Type Object
     */
    public abstract RequestScope getRequestScope(TableExport query, Elide elide, User user, String apiVersion);

    /**
     * Generate Download URL.
     * @param query TableExport type object.
     * @param scope RequestScope.
     * @return URL generated.
     */
    public String generateDownloadURL(TableExport query, RequestScope scope) {
        // TODO Add logic Here
        return null;
    }
}
