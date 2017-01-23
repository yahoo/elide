/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.security.RequestScope;

import java.util.Optional;

/**
 * The bridgeable transaction describes an interface suitable for handling composite models
 * across stores. More concretely, consider the following example model:
 *
 * <code>
 *   public class User {
 *     String name;
 *     MyOtherObject otherObject;
 *   }
 * </code>
 *
 * If we assume <em>User</em> is managed by a MySQL datastore and the <em>MyOtherObject</em> entity is managed by a
 * Redis datastore, the MultiplexManager will use a bridgeable store if applicable. The bridgeable store enables
 * the writer to determine how to lookup a cross-store relationship via key construction and transaction selection.
 *
 * <strong>NOTE:</strong> that since Elide binds a particular <em>type</em> to a datastore attributes will be looked up
 * using the datastore reasonable for managing that entity.
 */
public interface BridgeableTransaction {

    Object bridgeableLoadObject(MultiplexTransaction muxTx,
                                Object parent,
                                String relationName,
                                Optional<FilterExpression> filterExpression,
                                RequestScope scope);

    Object bridgeableLoadObjects(MultiplexTransaction muxTx,
                                 Object parent,
                                 String relationName,
                                 Optional<FilterExpression> filterExpression,
                                 RequestScope scope);
}
