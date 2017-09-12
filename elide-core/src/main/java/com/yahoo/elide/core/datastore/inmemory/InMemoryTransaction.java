/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.InMemoryFilterVisitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Id;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * InMemoryDataStore transaction handler.
 */
@Slf4j
public class InMemoryTransaction implements DataStoreTransaction {
    private final Map<Class<?>, Map<String, Object>> dataStore;
    private final List<Operation> operations;
    private final EntityDictionary dictionary;
    private final Map<Class<?>, AtomicLong> typeIds;

    public InMemoryTransaction(Map<Class<?>, Map<String, Object>> dataStore,
                               EntityDictionary dictionary, Map<Class<?>, AtomicLong> typeIds) {
        this.dataStore = dataStore;
        this.dictionary = dictionary;
        this.operations = new ArrayList<>();
        this.typeIds = typeIds;
    }

    @Override
    public void flush(RequestScope requestScope) {
        // Do nothing
    }

    @Override
    public void save(Object object, RequestScope requestScope) {
        if (object == null) {
            return;
        }
        String id = dictionary.getId(object);
        if (id == null || "null".equals(id) || "0".equals(id)) {
            createObject(object, requestScope);
        }
        id = dictionary.getId(object);
        operations.add(new Operation(id, object, object.getClass(), false));
    }

    @Override
    public void delete(Object object, RequestScope requestScope) {
        if (object == null) {
            return;
        }

        String id = dictionary.getId(object);
        operations.add(new Operation(id, object, object.getClass(), true));
    }

    @Override
    public void commit(RequestScope scope) {
        synchronized (dataStore) {
            operations.stream()
                    .filter(op -> op.getInstance() != null)
                    .forEach(op -> {
                        Object instance = op.getInstance();
                        String id = op.getId();
                        Map<String, Object> data = dataStore.get(op.getType());
                        if (op.getDelete()) {
                            data.remove(id);
                        } else {
                            data.put(id, instance);
                        }
                    });
            operations.clear();
        }
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        Class entityClass = entity.getClass();

        AtomicLong nextId = typeIds.computeIfAbsent(entityClass,
                (key) -> { return new AtomicLong(1); });
        String id = String.valueOf(nextId.getAndIncrement());
        setId(entity, id);
        operations.add(new Operation(id, entity, entity.getClass(), false));
    }

    private AtomicLong newRandomId(Class<?> ignored) {
        return new AtomicLong(ThreadLocalRandom.current().nextLong());
    }

    public void setId(Object value, String id) {
        for (Class<?> cls = value.getClass(); cls != null; cls = cls.getSuperclass()) {
            for (Method method : cls.getMethods()) {
                if (method.isAnnotationPresent(Id.class) && method.getName().startsWith("get")) {
                    String setName = "set" + method.getName().substring(3);
                    for (Method setMethod : cls.getMethods()) {
                        if (setMethod.getName().equals(setName) && setMethod.getParameterCount() == 1) {
                            try {
                                setMethod.invoke(value, CoerceUtil.coerce(id, setMethod.getParameters()[0].getType()));
                            } catch (ReflectiveOperationException e) {
                                log.error("set {}", setMethod, e);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public Object getRelation(DataStoreTransaction relationTx,
                              Object entity,
                              String relationName,
                              Optional<FilterExpression> filterExpression,
                              Optional<Sorting> sorting,
                              Optional<Pagination> pagination,
                              RequestScope scope) {
        Object values = PersistentResource.getValue(entity, relationName, scope);
        Class childClass = dictionary.getParameterizedType(entity, relationName);
        Class childIdType = dictionary.getIdType(childClass);
        String childIdField = dictionary.getIdFieldName(childClass);

        // Gather list of valid id's from this parent
        List<Object> validChildIds;
        if (dictionary.getRelationshipType(entity, relationName).isToOne()) {
            if (values == null) {
                return null;
            }
            validChildIds = Arrays.asList(dictionary.getId(values));
        } else if (values instanceof Collection) {
            List<String> ids = (List<String>) ((Collection) values).stream()
                    .map(v -> dictionary.getId(v))
                    .collect(Collectors.toList());
            validChildIds = new ArrayList<>(ids);
        } else {
            throw new IllegalStateException("An unexpected error occurred querying a relationship");
        }

        FilterExpression childIdFilter = new FilterPredicate(new FilterPredicate.PathElement(
                childClass,
                childIdType,
                childIdField
        ), Operator.IN, validChildIds);

        FilterExpression joinedFilter = filterExpression
                .map(fe -> (FilterExpression) new AndFilterExpression(childIdFilter, fe))
                .orElse(childIdFilter);

        Class entityClass = dictionary.getParameterizedType(entity, relationName);

        return loadObjects(entityClass, Optional.of(joinedFilter), sorting, pagination, scope);
    }

    @Override
    public Object loadObject(Class<?> entityClass, Serializable id,
                             Optional<FilterExpression> filterExpression, RequestScope scope) {
        Class idType = dictionary.getIdType(entityClass);
        String idField = dictionary.getIdFieldName(entityClass);
        FilterExpression idFilter = new FilterPredicate(
                new FilterPredicate.PathElement(entityClass, idType, idField),
                Operator.IN,
                Arrays.asList(id)
        );
        FilterExpression joinedFilterExpression = filterExpression
                .map(fe -> (FilterExpression) new AndFilterExpression(idFilter, fe))
                .orElse(idFilter);
        Iterable<Object> results = loadObjects(entityClass,
                Optional.of(joinedFilterExpression),
                Optional.empty(),
                Optional.empty(),
                scope);
        Iterator<Object> it = results == null ? null : results.iterator();
        if (it != null && it.hasNext()) {
            return it.next();
        }
        return null;
    }

    @Override
    public Iterable<Object> loadObjects(Class<?> entityClass, Optional<FilterExpression> filterExpression,
                                        Optional<Sorting> sorting, Optional<Pagination> pagination,
                                        RequestScope scope) {
        synchronized (dataStore) {
            Map<String, Object> data = dataStore.get(entityClass);

            // Support for filtering
            List<Object> results = filterExpression
                    .map(fe -> {
                        Predicate predicate = fe.accept(new InMemoryFilterVisitor(scope));
                        return data.values().stream().filter(predicate::test).collect(Collectors.toList());
                    })
                    .orElseGet(() -> new ArrayList<>(data.values()));

            // Support for sorting
            Comparator<Object> noSort = (left, right) -> 0;
            List<Object> sorted = sorting
                    .map(sort -> {
                        Map<Path, Sorting.SortOrder> sortRules = sort.getValidSortingRules(entityClass, dictionary);
                        if (sortRules.isEmpty()) {
                            // No sorting
                            return results;
                        }
                        Comparator<Object> comp = sortRules.entrySet().stream()
                                .map(entry -> getComparator(entry.getKey(), entry.getValue(), scope))
                                .reduce(noSort, (first, second) -> (left, right) -> {
                                    int comparison = first.compare(left, right);
                                    if (comparison == 0) {
                                        return second.compare(left, right);
                                    }
                                    return comparison;
                                });
                        results.sort(comp);
                        return results;
                    })
                    .orElse(results);

            // Support for pagination. Should be done _after_ filtering
            return pagination
                    .map(p -> {
                        int offset = p.getOffset();
                        int limit = p.getLimit();
                        if (offset < 0 || offset >= sorted.size()) {
                            return Collections.emptyList();
                        }
                        int endIdx = offset + limit;
                        if (endIdx > sorted.size()) {
                            endIdx = sorted.size();
                        }
                        return sorted.subList(offset, endIdx);
                    })
                    .orElse(sorted);
        }
    }

    @Override
    public void close() throws IOException {
        operations.clear();
    }

    /**
     * Get the comparator for sorting.
     *
     * @param path Path to field for sorting
     * @param order Order to sort
     * @param requestScope Request scope
     * @return Comparator for sorting
     */
    private Comparator<Object> getComparator(Path path, Sorting.SortOrder order, RequestScope requestScope) {
        return (left, right) -> {
            Object leftCompare = left;
            Object rightCompare = right;

            // Drill down into path to find value for comparison
            for (Path.PathElement pathElement : path.getPathElements()) {
                leftCompare = PersistentResource.getValue(leftCompare, pathElement.getFieldName(), requestScope);
                rightCompare = PersistentResource.getValue(rightCompare, pathElement.getFieldName(), requestScope);
            }

            // Make sure value is comparable and perform comparison
            if (leftCompare instanceof Comparable) {
                int result = ((Comparable<Object>) leftCompare).compareTo(rightCompare);
                if (order == Sorting.SortOrder.asc) {
                    return result;
                }
                return -result;
            }

            throw new IllegalStateException("Trying to comparing non-comparable types!");
        };
    }
}
