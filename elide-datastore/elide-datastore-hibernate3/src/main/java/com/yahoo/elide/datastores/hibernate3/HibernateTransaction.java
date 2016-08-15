/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.hibernate3.filter.CriterionFilterOperation;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.User;

import com.google.common.base.Objects;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.collection.AbstractPersistentCollection;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Hibernate Transaction implementation.
 */
public class HibernateTransaction implements DataStoreTransaction {

    private final Session session;
    private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();
    private final boolean isScrollEnabled;
    private final ScrollMode scrollMode;

    /**
     * Constructor.
     *
     * @param session Hibernate session
     * @param isScrollEnabled Whether or not scrolling is enabled
     * @param scrollMode Scroll mode to use if scrolling enabled
     */
    protected HibernateTransaction(Session session, boolean isScrollEnabled, ScrollMode scrollMode) {
        this.session = session;
        this.isScrollEnabled = isScrollEnabled;
        this.scrollMode = scrollMode;
    }

    @Override
    public void delete(Object object, RequestScope scope) {
        deferredTasks.add(() -> session.delete(object));
    }

    @Override
    public void save(Object object, RequestScope scope) {
        deferredTasks.add(() -> session.saveOrUpdate(object));
    }

    @Override
    public void flush(RequestScope requestScope) {
        try {
            deferredTasks.forEach(Runnable::run);
            deferredTasks.clear();
            session.flush();
        } catch (HibernateException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public void commit(RequestScope requestScope) {
        try {
            this.flush(requestScope);
            this.session.getTransaction().commit();
        } catch (HibernateException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        deferredTasks.add(() -> session.persist(entity));
    }

    /**
     * load a single record with id and filter.
     *
     * @param entityClass class of query object
     * @param id id of the query object
     * @param filterExpression FilterExpression contains the predicates
     */
    @Override
    public Object loadObject(Class<?> entityClass,
                             Serializable id,
                             Optional<FilterExpression> filterExpression,
                             RequestScope scope) {
        try {
            Criteria criteria = session.createCriteria(entityClass).add(Restrictions.idEq(id));
            if (scope != null && isJoinQuery()) {
                joinCriteria(criteria, entityClass, scope);
            }
            if (filterExpression.isPresent()) {
                CriterionFilterOperation filterOpn = new CriterionFilterOperation(criteria);
                criteria = filterOpn.apply(filterExpression.get());
            }
            Object record = criteria.uniqueResult();
            return record;
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    @Override
    public Iterable<Object> loadObjects(
            Class<?> entityClass,
            Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope) {
        com.yahoo.elide.core.RequestScope requestScope;
        try {
            requestScope  = (com.yahoo.elide.core.RequestScope) scope;
        } catch (ClassCastException e) {
            throw new ClassCastException("Fail trying to cast requestscope");
        }
        Criteria criteria = session.createCriteria(entityClass);

        if (filterExpression.isPresent()) {
            CriterionFilterOperation filterOpn = new CriterionFilterOperation(criteria);
            criteria = filterOpn.apply(filterExpression.get());
        }

        Set<Order> validatedSortingRules = null;
        if (sorting.isPresent()) {
            if (!sorting.get().isDefaultInstance()) {
                final EntityDictionary dictionary = requestScope.getDictionary();
                validatedSortingRules = sorting.get().getValidSortingRules(entityClass, dictionary).entrySet()
                        .stream()
                        .map(entry -> entry.getValue().equals(Sorting.SortOrder.desc)
                                ? Order.desc(entry.getKey())
                                : Order.asc(entry.getKey())
                        )
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
        }

        return loadObjects(
                entityClass,
                criteria,
                Optional.ofNullable(validatedSortingRules),
                pagination,
                scope);
    }

    /**
     * Generates the Hibernate ScrollableIterator for Hibernate Query.
     * @param loadClass The hibernate class to build the query off of.
     * @param criteria The criteria to use for filters
     * @param sortingRules The possibly empty sorting rules.
     * @param pagination The Optional pagination object.
     * @return The Iterable for Hibernate.
     */
    public Iterable loadObjects(final Class<?> loadClass, final Criteria criteria,
            final Optional<Set<Order>> sortingRules, final Optional<Pagination> pagination, RequestScope scope) {
        if (sortingRules.isPresent()) {
            sortingRules.get().forEach(criteria::addOrder);
        }

        if (pagination.isPresent()) {
            final Pagination paginationData = pagination.get();
            paginationData.evaluate(loadClass);
            criteria.setFirstResult(paginationData.getOffset());
            criteria.setMaxResults(paginationData.getLimit());
        } else {
            Integer queryLimit = getQueryLimit();
            if (queryLimit != null) {
                criteria.setMaxResults(queryLimit);
            }
        }

        if (isJoinQuery()) {
            joinCriteria(criteria, loadClass, scope);
        }

        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        if (!isScrollEnabled || isJoinQuery()) {
            return criteria.list();
        }
        return new ScrollableIterator(criteria.scroll(scrollMode));
    }

    @Override
    public <T> Long getTotalRecords(Class<T> entityClass) {
        final Criteria sessionCriteria = session.createCriteria(entityClass);
        sessionCriteria.setProjection(Projections.rowCount());
        return (Long) sessionCriteria.uniqueResult();
    }

    /**
     * Should this transaction use JOINs. Override to force joins.
     *
     * @return true to use join logic
     */
    public boolean isJoinQuery() {
        return false;
    }

    private <T> void joinCriteria(Criteria criteria, final Class<T> loadClass, RequestScope scope) {
        com.yahoo.elide.core.RequestScope requestScope;
        try {
            requestScope  = (com.yahoo.elide.core.RequestScope) scope;
        } catch (ClassCastException e) {
            throw new ClassCastException("Fail trying to cast requestscope");
        }
        EntityDictionary dictionary = requestScope.getDictionary();
        String type = dictionary.getJsonAliasFor(loadClass);
        Set<String> fields = Objects.firstNonNull(
                requestScope.getSparseFields().get(type), Collections.<String>emptySet());
        for (String field : fields) {
            try {
                checkFieldReadPermission(loadClass, field, scope);
                criteria.setFetchMode(field, FetchMode.JOIN);
            } catch (ForbiddenAccessException e) {
                // continue
            }
        }

        for (String include : getIncludeList(scope)) {
            criteria.setFetchMode(include, FetchMode.JOIN);
        }
    }

    /**
     * Parse include param into list of include fields.
     * @return list of include fields
     */
    public List<String> getIncludeList(RequestScope scope) {
        com.yahoo.elide.core.RequestScope requestScope;
        try {
            requestScope  = (com.yahoo.elide.core.RequestScope) scope;
        } catch (ClassCastException e) {
            throw new ClassCastException("Fail trying to cast requestscope");
        }
        List<String> includeParam;
        if (!requestScope.getQueryParams().isPresent()) {
            return Collections.emptyList();
        }
        includeParam = requestScope.getQueryParams().get().get("include");
        if (includeParam == null || includeParam.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<String> list = new ArrayList<>();
        for (String includeList : includeParam) {
            for (String includeItem : includeList.split(",")) {
                for (int idx = 0; idx != -1;) {
                    idx = includeItem.indexOf('.', idx + 1);
                    String field = (idx == -1) ? includeItem : includeItem.substring(0, idx);
                    list.add(field);
                }
            }
        }
        return list;
    }

    private <T> void checkFieldReadPermission(final Class<T> loadClass, String field, RequestScope scope) {
        // wrap class as PersistentResource in order to check permission
        com.yahoo.elide.core.RequestScope requestScope;
        try {
            requestScope  = (com.yahoo.elide.core.RequestScope) scope;
        } catch (ClassCastException e) {
            throw new ClassCastException("Fail trying to cast requestscope");
        }
        PersistentResource<T> resource = new PersistentResource<T>() {
            @Override
            public boolean matchesId(String id) {
                return false;
            }

            @Override
            public Optional<String> getUUID() {
                return Optional.empty();
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public String getType() {
                return null;
            }

            @Override
            public T getObject() {
                return null;
            }

            @Override
            public Class<T> getResourceClass() {
                return loadClass;
            }

            @Override
            public com.yahoo.elide.security.RequestScope getRequestScope() {
                return requestScope;
            }
        };

        requestScope.getPermissionExecutor().checkUserPermissions(resource, ReadPermission.class, field);
    }

    public Object getRelation(
            DataStoreTransaction relationTx,
            Object entity,
            String relationName,
            Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope) {
        com.yahoo.elide.core.RequestScope requestScope;
        try {
            requestScope  = (com.yahoo.elide.core.RequestScope) scope;
        } catch (ClassCastException e) {
            throw new ClassCastException("Fail trying to cast requestscope");
        }
        EntityDictionary dictionary = requestScope.getDictionary();
        Object val = com.yahoo.elide.core.PersistentResource.getValue(entity, relationName, dictionary);
        if (val instanceof Collection) {
            Collection filteredVal = (Collection) val;
            if (filteredVal instanceof AbstractPersistentCollection) {
                @SuppressWarnings("unchecked")
                Class<?> relationClass = dictionary.getParameterizedType(entity, relationName);
                final Optional<Query> possibleQuery =
                        new HQLTransaction.Builder<>(session, filteredVal, relationClass,
                                dictionary)
                                .withPossibleFilterExpression(filterExpression)
                                .withPossibleSorting(sorting)
                                .withPossiblePagination(pagination)
                                .build();

                if (possibleQuery.isPresent()) {
                    return possibleQuery.get().list();
                }
            }
        }
        return val;
    }

    @Override
    public void close() throws IOException {
        if (session.isOpen() && session.getTransaction().isActive()) {
            session.getTransaction().rollback();
            throw new IOException("Transaction not closed");
        }
    }

    @Override
    public User accessUser(Object opaqueUser) {
        return new User(opaqueUser);
    }

    public Integer getQueryLimit() {
        // no limit
        return null;
    }
}
