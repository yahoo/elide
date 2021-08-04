/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;

import javax.jms.JMSContext;

import java.io.IOException;

public class JMSDataStoreTransaction implements DataStoreTransaction {
    private JMSContext context;

    public JMSDataStoreTransaction(JMSContext context) {
        this.context = context;
    }

    @Override
    public <T> void save(T entity, RequestScope scope) {
        throw new BadRequestException("Unsupported operation");
    }

    @Override
    public <T> void delete(T entity, RequestScope scope) {
        throw new BadRequestException("Unsupported operation");
    }

    @Override
    public void flush(RequestScope scope) {

    }

    @Override
    public void commit(RequestScope scope) {

    }

    @Override
    public <T> void createObject(T entity, RequestScope scope) {
        throw new BadRequestException("Unsupported operation");
    }

    @Override
    public <T> Iterable<T> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        context.createQueue()
        return null;
    }

    @Override
    public void cancel(RequestScope scope) {

    }

    @Override
    public void close() throws IOException {

    }
}
