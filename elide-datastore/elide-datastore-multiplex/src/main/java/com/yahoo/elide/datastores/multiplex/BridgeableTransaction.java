/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

import java.io.Serializable;
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
 *
 * <strong>N.B.</strong> this interface should be implemented on the relevant store in which a particular object lives.
 *           That is, considering the example above, the Redis store would implement this interface to bridge
 *           automagically from MySQL.
 */
public interface BridgeableTransaction {

    /**
     * Load a single object from a bridgeable store.
     *
     * <em>NOTE:</em> The filter expression will be pre-populated with an ID-based lookup from Elide.
     *       This filter expression is constructed with the id passed in the query URL.
     *
     * @param muxTx  Multiplex transaction
     * @param parent  Parent object
     * @param relationName  Relation name on parent to expected entity
     * @param filterExpression  Filter expression to apply to query
     * @param lookupId  Id of entity intended to be looked up
     *                  <strong>N.B.</strong> This value <em>may</em> be null if called through a to-one relationship
     *                  and an explicit id was not provided. In such a case, it is expected that the datastore
     *                  can derive the appropriate id with the other provided information.
     * @param scope  Request scope
     * @return Loaded object from bridgeable store.
     */
    Object bridgeableLoadObject(MultiplexTransaction muxTx,
                                Object parent,
                                String relationName,
                                Serializable lookupId,
                                Optional<FilterExpression> filterExpression,
                                RequestScope scope);

    /**
     * Load a collection of objects from a bridgeable store.
     *
     * @param muxTx  Multiplex transaction
     * @param parent  Parent object
     * @param relationName  Relation name on parent to expected entity
     * @param filterExpression  Filter expression to apply to query
     * @param sorting  Sorting method for collection
     * @param pagination  Pagination for collection
     * @param scope  Request scope
     * @return Loaded iterable of objects from bridgeable store.
     */
    Iterable<Object> bridgeableLoadObjects(MultiplexTransaction muxTx,
                                 Object parent,
                                 String relationName,
                                 Optional<FilterExpression> filterExpression,
                                 Optional<Sorting> sorting,
                                 Optional<Pagination> pagination,
                                 RequestScope scope);
}
