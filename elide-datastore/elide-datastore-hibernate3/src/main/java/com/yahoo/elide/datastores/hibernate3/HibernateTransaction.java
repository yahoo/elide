/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.google.common.base.Objects;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.FilterScope;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.RequestScopedTransaction;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.hibernate3.filter.CriteriaExplorer;
import com.yahoo.elide.datastores.hibernate3.filter.CriterionFilterOperation;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.User;
import lombok.AccessLevel;
import lombok.Getter;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.collection.AbstractPersistentCollection;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Hibernate Transaction implementation.
 */
public class HibernateTransaction implements RequestScopedTransaction {
    private static final Function<Criterion, Criterion> NOT = Restrictions::not;
    private static final BiFunction<Criterion, Criterion, Criterion> AND = Restrictions::and;
    private static final BiFunction<Criterion, Criterion, Criterion> OR = Restrictions::or;

    private final Session session;
    private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();
    private final CriterionFilterOperation criterionFilterOperation = new CriterionFilterOperation();
    private final boolean isScrollEnabled;
    private final ScrollMode scrollMode;
    @Getter(value = AccessLevel.PROTECTED)
    private RequestScope requestScope = null;

    /**
     * Instantiates a new Hibernate transaction.
     *
     * @param session the session
     * @deprecated since 2.3.2. Will be removed no later than the release of Elide 3.0.
     */
    @Deprecated
    public HibernateTransaction(Session session) {
        this.session = session;
        this.isScrollEnabled = true;
        this.scrollMode = ScrollMode.FORWARD_ONLY;
    }

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
    public void delete(Object object) {
        deferredTasks.add(() -> session.delete(object));
    }

    @Override
    public void save(Object object) {
        deferredTasks.add(() -> session.saveOrUpdate(object));
    }

    @Override
    public void flush() {
        try {
            deferredTasks.forEach(Runnable::run);
            deferredTasks.clear();
            session.flush();
        } catch (HibernateException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public void commit() {
        try {
            this.flush();
            this.session.getTransaction().commit();
        } catch (HibernateException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public <T> T createObject(Class<T> entityClass) {
        try {
            T object = entityClass.newInstance();
            deferredTasks.add(() -> session.persist(object));
            return object;
        } catch (java.lang.InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public <T> T loadObject(Class<T> loadClass, Serializable id) {
        try {
            if (isJoinQuery()) {
                Criteria criteria = session.createCriteria(loadClass).add(Restrictions.idEq(id));
                if (requestScope != null) {
                    joinCriteria(criteria, loadClass);
                }
                @SuppressWarnings("unchecked")
                T record = (T) criteria.uniqueResult();
                return record;
            }
            @SuppressWarnings("unchecked")
            T record = (T) session.load(loadClass, id);
            Hibernate.initialize(record);
            return record;
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    @Override
    public <T> Iterable<T> loadObjects(Class<T> loadClass) {
        throw new IllegalStateException("" + loadClass);
    }

    @Override
    public <T> Iterable<T> loadObjects(Class<T> loadClass, FilterScope filterScope) {
        Criterion criterion = filterScope.getCriterion(NOT, AND, OR);

        // Criteria for filtering this object
        CriteriaExplorer criteriaExplorer = new CriteriaExplorer(loadClass, filterScope.getRequestScope(), criterion);

        return loadObjects(loadClass, criteriaExplorer, Optional.empty(), Optional.empty());
    }

    @Override
    public <T> Iterable<T> loadObjectsWithSortingAndPagination(Class<T> entityClass, FilterScope filterScope) {
        Criterion criterion = filterScope.getCriterion(NOT, AND, OR);

        String type = filterScope.getRequestScope().getDictionary().getJsonAliasFor(entityClass);
        Set<Predicate> filteredPredicates = filterScope.getRequestScope().getPredicatesOfType(type);
        criterion = CriterionFilterOperation.andWithNull(
                criterion,
                criterionFilterOperation.applyAll(filteredPredicates)
        );


        final Pagination pagination = filterScope.hasPagination()
                ? filterScope.getRequestScope().getPagination()
                : null;

        // if we have sorting and sorting isn't empty, then we should pull dictionary to validate the sorting rules
        Set<Order> validatedSortingRules = null;
        if (filterScope.hasSortingRules()) {
            final Sorting sorting = filterScope.getRequestScope().getSorting();
            final EntityDictionary dictionary = filterScope.getRequestScope().getDictionary();
            validatedSortingRules = sorting.getValidSortingRules(entityClass, dictionary).entrySet()
                    .stream()
                    .map(entry -> entry.getValue().equals(Sorting.SortOrder.desc)
                            ? Order.desc(entry.getKey())
                            : Order.asc(entry.getKey())
                    )
                    .collect(Collectors.toSet());
        }

        return loadObjects(entityClass, new CriteriaExplorer(entityClass, filterScope.getRequestScope(), criterion),
                Optional.ofNullable(validatedSortingRules), Optional.ofNullable(pagination));
    }

    /**
     * Generates the Hibernate ScrollableIterator for Hibernate Query.
     * @param loadClass The hibernate class to build the query off of.
     * @param criteriaExplorer Criteria explorer to explore and construct criterion
     * @param sortingRules The possibly empty sorting rules.
     * @param pagination The Optional pagination object.
     * @param <T> The return Iterable type.
     * @return The Iterable for Hibernate.
     */
    public <T> Iterable<T> loadObjects(final Class<T> loadClass, final CriteriaExplorer criteriaExplorer,
            final Optional<Set<Order>> sortingRules, final Optional<Pagination> pagination) {
        final Criteria sessionCriteria = session.createCriteria(loadClass);

        criteriaExplorer.buildCriteria(sessionCriteria, session);

        if (sortingRules.isPresent()) {
            sortingRules.get().forEach(sessionCriteria::addOrder);
        }

        if (pagination.isPresent()) {
            final Pagination paginationData = pagination.get();
            sessionCriteria.setFirstResult(paginationData.getOffset());
            sessionCriteria.setMaxResults(paginationData.getLimit());
        } else {
            Integer queryLimit = getQueryLimit();
            if (queryLimit != null) {
                sessionCriteria.setMaxResults(queryLimit);
            }
        }

        if (isJoinQuery()) {
            joinCriteria(sessionCriteria, loadClass);
        }

        sessionCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        if (!isScrollEnabled || isJoinQuery()) {
            return sessionCriteria.list();
        }
        return new ScrollableIterator(sessionCriteria.scroll(scrollMode));
    }

    /**
     * Should this transaction use JOINs. Override to force joins.
     *
     * @return true to use join logic
     */
    public boolean isJoinQuery() {
        return false;
    }

    private <T> void joinCriteria(Criteria criteria, final Class<T> loadClass) {
        EntityDictionary dictionary = requestScope.getDictionary();
        String type = dictionary.getJsonAliasFor(loadClass);
        Set<String> fields = Objects.firstNonNull(
                requestScope.getSparseFields().get(type), Collections.<String>emptySet());
        for (String field : fields) {
            try {
                checkFieldReadPermission(loadClass, field);
                criteria.setFetchMode(field, FetchMode.JOIN);
            } catch (ForbiddenAccessException e) {
                // continue
            }
        }

        for (String include : getIncludeList()) {
            criteria.setFetchMode(include, FetchMode.JOIN);
        }
    }

    /**
     * Parse include param into list of include fields.
     * @return list of include fields
     */
    public List<String> getIncludeList() {
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

    private <T> void checkFieldReadPermission(final Class<T> loadClass, String field) {
        // wrap class as PersistentResource in order to check permission
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

    @Override
    public <T> Collection filterCollection(Collection collection, Class<T> entityClass, Set<Predicate> predicates) {
        if ((collection instanceof AbstractPersistentCollection) && !predicates.isEmpty()) {
            String filterString = new HQLFilterOperation().applyAll(predicates);

            if (filterString.length() != 0) {
                Query query = session.createFilter(collection, filterString);

                for (Predicate predicate : predicates) {
                    if (predicate.getOperator().isParameterized()) {
                        query = query.setParameterList(predicate.getField(), predicate.getValues());
                    }
                }

                return query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
            }
        }

        return collection;
    }

    @Override
    public <T> Collection filterCollectionWithSortingAndPagination(final Collection collection,
                                                                   final Class<T> entityClass,
                                                                   final EntityDictionary dictionary,
                                                                   final Optional<Set<Predicate>> filters,
                                                                   final Optional<Sorting> sorting,
                                                                   final Optional<Pagination> pagination) {
        if (((collection instanceof AbstractPersistentCollection))
                && (filters.isPresent() || sorting.isPresent() || pagination.isPresent())) {
            @SuppressWarnings("unchecked")
            final Optional<Query> possibleQuery = new HQLTransaction.Builder<>(session, collection, entityClass,
                    dictionary)
                    .withPossibleFilters(filters)
                    .withPossibleSorting(sorting)
                    .withPossiblePagination(pagination)
                    .build();
            if (possibleQuery.isPresent()) {
                return possibleQuery.get().list();
            }
        }
        return collection;
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

    @Override
    public void setRequestScope(RequestScope requestScope) {
        this.requestScope = requestScope;
    }

    public Integer getQueryLimit() {
        // no limit
        return null;
    }
}
