/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex.bridgeable;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.multiplex.BridgeableStoreTest;
import com.yahoo.elide.datastores.multiplex.BridgeableTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexTransaction;
import com.yahoo.elide.example.beans.HibernateUser;
import com.yahoo.elide.example.hbase.beans.RedisActions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class BridgeableRedisStore implements DataStore {
    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        dictionary.bindEntity(RedisActions.class);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new ExampleRedisTransaction();
    }

    @Override
    public DataStoreTransaction beginReadTransaction() {
        return new ExampleRedisTransaction();
    }

    public class ExampleRedisTransaction implements BridgeableTransaction, DataStoreTransaction {

        @Override
        public <T> T loadObject(EntityProjection projection,
                                 Serializable id,
                                 RequestScope scope) {
            if (!projection.getType().equals(ClassType.of(RedisActions.class))) {
                log.debug("Tried to load unexpected object from redis: {}", projection.getType());
                throw new RuntimeException("Tried to load unexpected object from redis!");
            }

            String key = RedisActions.class.getCanonicalName();

            FilterExpression fe = projection.getFilterExpression();
            if (fe != null) {
                RedisFilter filter = fe.accept(new FilterExpressionParser());
                if ("user_id".equals(filter.getFieldName())) {
                    Iterable<?> values = fetchValues(key,
                            v -> v.equals("user" + filter.getValues().get(0) + ":" + id));
                    for (Object value : values) {
                        return (T) value;
                    }
                }
                return null;
            }

            return (T) fetchValues(key, v -> id.equals(v.split(":")[1]));
        }

        @Override
        public <T> Iterable<T> loadObjects(EntityProjection projection,
                                            RequestScope scope) {
            if (!projection.getType().equals(ClassType.of(RedisActions.class))) {
                log.debug("Tried to load unexpected object from redis: {}", projection.getType());
                throw new RuntimeException("Tried to load unexpected object from redis!");
            }

            String key = RedisActions.class.getCanonicalName();

            return (Iterable<T>) Optional.ofNullable(projection.getFilterExpression())
                    .map(fe -> {
                        RedisFilter filter = fe.accept(new FilterExpressionParser());
                        if ("user_id".equals(filter.getFieldName())) {
                            return fetchValues(key,
                                    v -> v.startsWith("user" + filter.getValues().get(0) + ":"));
                        }
                        log.error("Received bad filter: {} for type: {}", filter, projection.getType());
                        throw new UnsupportedOperationException("Cannot filter object of that type");
                    })
                    .orElseGet(() -> fetchValues(key, unused -> true));
        }

        private Iterable<Object> fetchValues(String key, java.util.function.Predicate<String> filterVal) {
            Jedis client = BridgeableStoreTest.REDIS_CLIENT;
            return client.hgetAll(key).entrySet().stream()
                    .filter(e -> filterVal.test(e.getKey()))
                    .map(this::deserializeAction)
                    .collect(Collectors.toList());
        }

        private RedisActions deserializeAction(Map.Entry<String, String> entry) {
            String[] idParts = entry.getKey().split(":");
            String actionId = idParts[1];

            RedisActions action = new RedisActions();
            action.setId(actionId);
            action.setDescription(entry.getValue());

            return action;
        }

        // ---- Bridgeable Interfaces ----

        @Override
        public Object bridgeableLoadObject(MultiplexTransaction muxTx, Object parent, String relationName, Serializable lookupId, Optional<FilterExpression> filterExpression, RequestScope scope) {
            if (parent.getClass().equals(HibernateUser.class)) {
                EntityDictionary dictionary = scope.getDictionary();
                Type<?> entityClass = dictionary.getParameterizedType(parent, relationName);
                HibernateUser user = (HibernateUser) parent;
                if ("specialAction".equals(relationName)) {
                    return muxTx.loadObject(
                            EntityProjection.builder().type(entityClass).build(),
                            String.valueOf(user.getSpecialActionId()),
                            scope);
                }
                if ("redisActions".equals(relationName)) {
                    FilterExpression updatedExpression = new InPredicate(
                            new Path.PathElement(entityClass, ClassType.STRING_TYPE, "user_id"),
                            String.valueOf(((HibernateUser) parent).getId())
                    );

                    return muxTx.loadObject(EntityProjection.builder()
                            .type(entityClass)
                            .filterExpression(updatedExpression)
                            .build(),
                            String.valueOf(lookupId),
                            scope);
                }
            }
            log.error("Tried to bridge from parent: {} to relation name: {}", parent, relationName);
            throw new RuntimeException("Unsupported bridging attempted!");
        }

        @Override
        public Iterable<Object> bridgeableLoadObjects(MultiplexTransaction muxTx,
                                                      Object parent,
                                                      String relationName,
                                                      Optional<FilterExpression> filterExpressionOptional,
                                                      Optional<Sorting> sorting,
                                                      Optional<Pagination> pagination, RequestScope scope) {
            if (parent.getClass().equals(HibernateUser.class) && "redisActions".equals(relationName)) {
                EntityDictionary dictionary = scope.getDictionary();
                Type<?> entityClass = dictionary.getParameterizedType(parent, relationName);
                FilterExpression filterExpression = new InPredicate(
                        new Path.PathElement(entityClass, ClassType.STRING_TYPE, "user_id"),
                        String.valueOf(((HibernateUser) parent).getId())
                );
                return muxTx.loadObjects(EntityProjection.builder()
                        .type(entityClass)
                        .filterExpression(filterExpression)
                        .sorting(sorting.orElse(null))
                        .pagination(pagination.orElse(null))
                        .build(), scope);
            }
            log.error("Tried to bridge from parent: {} to relation name: {}", parent, relationName);
            throw new RuntimeException("Unsupported bridging attempted!");
        }

        // ---- Unsupported operations ----

        @Override
        public <T, R> R getRelation(DataStoreTransaction relationTx,
                                    T entity, Relationship relationship, RequestScope scope) {
            throw new UnsupportedOperationException("No redis relationships currently supported.");
        }

        @Override
        public <T, R> void updateToManyRelation(DataStoreTransaction relationTx, T entity, String relationName, Set<R> newRelationships, Set<R> deletedRelationships, RequestScope scope) {

        }

        @Override
        public <T, R> void updateToOneRelation(DataStoreTransaction relationTx, T entity, String relationName, R relationshipValue, RequestScope scope) {

        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public <T> void save(T entity, RequestScope scope) {

        }

        @Override
        public <T> void delete(T entity, RequestScope scope) {

        }

        @Override
        public void flush(RequestScope scope) {

        }

        @Override
        public void commit(RequestScope scope) {

        }

        @Override
        public void preCommit(RequestScope scope) {

        }

        @Override
        public <T> void createObject(T entity, RequestScope scope) {

        }

        @Override
        public <T> T createNewObject(Type<T> entityClass) {
            return null;
        }

        @Override
        public void cancel(RequestScope scope) {
            // Nothing
        }
    }

    /**
     * Example structure for parsing filter expression.
     */
    @AllArgsConstructor
    private static class RedisFilter {
        @Getter private final String fieldName;
        @Getter private final Operator operator;
        @Getter private final List<Object> values;
    }

    /**
     * Small example parser.
     */
    private static class FilterExpressionParser implements FilterExpressionVisitor<RedisFilter> {
        @Override
        public RedisFilter visitPredicate(FilterPredicate predicate) {
            return new RedisFilter(
                    predicate.getField(),
                    predicate.getOperator(),
                    predicate.getValues()
            );
        }

        @Override
        public RedisFilter visitAndExpression(AndFilterExpression expression) {
            throw new UnsupportedOperationException("Unsupported operation");
        }

        @Override
        public RedisFilter visitOrExpression(OrFilterExpression expression) {
            throw new UnsupportedOperationException("Unsupported operation");
        }

        @Override
        public RedisFilter visitNotExpression(NotFilterExpression expression) {
            throw new UnsupportedOperationException("Unsupported operation");
        }
    }
}
