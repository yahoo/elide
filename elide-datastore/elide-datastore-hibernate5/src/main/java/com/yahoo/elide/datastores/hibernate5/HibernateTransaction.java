/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.FilterScope;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.RequestScopedTransaction;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.InMemoryFilterOperation;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.InMemoryFilterVisitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.hibernate5.filter.CriterionFilterOperation;
import com.yahoo.elide.extensions.PatchRequestScope;
import com.yahoo.elide.security.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
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
    private final boolean isScrollEnabled;
    private final ScrollMode scrollMode;
    @Getter(value = AccessLevel.PROTECTED)
    @Setter
    private RequestScope requestScope = null;

    /**
     * Instantiates a new Hibernate transaction.
     *
     * @param session the session
     * @deprecated since Elide 2.3.2. Will be removed no later than the release of Elide 3.0.
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

    /**
     * load a single record with id and filter.
     *
     * @param loadClass class of query object
     * @param id id of the query object
     * @param filterExpression FilterExpression contains the predicates
     */
    @Override
    public <T> T loadObject(Class<T> loadClass, Serializable id,  Optional<FilterExpression> filterExpression) {

        try {
            if (!filterExpression.isPresent()) {
                T record = session.get(loadClass, id);
                return record;
            }
            Criteria criteria = session.createCriteria(loadClass).add(Restrictions.idEq(id));
            CriterionFilterOperation filterOpn = buildCriterionFilterOperation(criteria);
            criteria = filterOpn.apply(filterExpression.get());
            T record = (T) criteria.uniqueResult();
            return record;
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    @Override
    public <T> T loadObject(Class<T> loadClass, Serializable id) {
        try {
            T record = session.get(loadClass, id);
            return record;
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    /**
     * Build the CriterionFilterOperation for provided criteria
     * @param criteria the criteria
     * @return the CriterionFilterOperation
     */
    protected CriterionFilterOperation buildCriterionFilterOperation(Criteria criteria) {
        return new CriterionFilterOperation(criteria);
    }

    @Deprecated
    @Override
    public <T> Iterable<T> loadObjects(Class<T> loadClass) {
        final Criteria sessionCriteria = session.createCriteria(loadClass);
        if (isScrollEnabled) {
            return new ScrollableIterator(sessionCriteria.scroll(scrollMode));
        }
        return sessionCriteria.list();
    }

    @Override
    public <T> Iterable<T> loadObjects(Class<T> loadClass, FilterScope filterScope) {
        Criterion securityCriterion = filterScope.getCriterion(NOT, AND, OR);

        Optional<FilterExpression> filterExpression = filterScope.getRequestScope().getLoadFilterExpression(loadClass);

        Criteria criteria = session.createCriteria(loadClass);
        if (securityCriterion != null) {
            criteria.add(securityCriterion);
        }

        if (filterExpression.isPresent()) {
            CriterionFilterOperation filterOpn = buildCriterionFilterOperation(criteria);
            criteria = filterOpn.apply(filterExpression.get());
        }

        return loadObjects(loadClass, criteria, Optional.empty(), Optional.empty());
    }

    @Override
    public <T> Iterable<T> loadObjectsWithSortingAndPagination(Class<T> entityClass, FilterScope filterScope) {
        Criterion securityCriterion = filterScope.getCriterion(NOT, AND, OR);

        Optional<FilterExpression> filterExpression =
                filterScope.getRequestScope().getLoadFilterExpression(entityClass);

        Criteria criteria = session.createCriteria(entityClass);
        if (securityCriterion != null) {
            criteria.add(securityCriterion);
        }

        if (filterExpression.isPresent()) {
            CriterionFilterOperation filterOpn = buildCriterionFilterOperation(criteria);
            criteria = filterOpn.apply(filterExpression.get());
        }

        final Pagination pagination = filterScope.getRequestScope().getPagination();

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
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        return loadObjects(
                entityClass,
                criteria,
                Optional.ofNullable(validatedSortingRules),
                Optional.ofNullable(pagination));
    }

    /**
     * Generates the Hibernate ScrollableIterator for Hibernate Query.
     * @param loadClass The hibernate class to build the query off of.
     * @param criteria The criteria to use for filters
     * @param sortingRules The possibly empty sorting rules.
     * @param pagination The Optional pagination object.
     * @param <T> The return Iterable type.
     * @return The Iterable for Hibernate.
     */
    public <T> Iterable<T> loadObjects(final Class<T> loadClass, final Criteria criteria,
            final Optional<Set<Order>> sortingRules, final Optional<Pagination> pagination) {
        if (sortingRules.isPresent()) {
            sortingRules.get().forEach(criteria::addOrder);
        }

        if (pagination.isPresent()) {
            final Pagination paginationData = pagination.get();
            paginationData.evaluate(loadClass);
            criteria.setFirstResult(paginationData.getOffset());
            criteria.setMaxResults(paginationData.getLimit());
        }

        if (isScrollEnabled) {
            return new ScrollableIterator(criteria.scroll(scrollMode));
        }
        return criteria.list();
    }

    @Override
    public <T> Object getRelation(
            Object entity,
            RelationshipType relationshipType,
            String relationName,
            Class<T> relationClass,
            EntityDictionary dictionary,
            Optional<FilterExpression> filterExpression,
            Sorting sorting,
            Pagination pagination
    ) {
        Object val = com.yahoo.elide.core.PersistentResource.getValue(entity, relationName, dictionary);
        if (val instanceof Collection) {
            Collection filteredVal = (Collection) val;
            if (filteredVal instanceof AbstractPersistentCollection) {
                if (getRequestScope() instanceof PatchRequestScope && filterExpression.isPresent()) {
                    return patchRequestFilterCollection(filteredVal, relationClass, filterExpression.get());
                }

                Optional<Sorting> sortingRules = sorting != null ? Optional.of(sorting) : Optional.empty();
                Optional<Pagination> paginationRules = pagination != null ? Optional.of(pagination) : Optional.empty();

                @SuppressWarnings("unchecked")
                final Optional<Query> possibleQuery = new HQLTransaction.Builder<>(session, filteredVal, relationClass,
                        dictionary)
                        .withPossibleFilterExpression(filterExpression)
                        .withPossibleSorting(sortingRules)
                        .withPossiblePagination(paginationRules)
                        .build();
                if (possibleQuery.isPresent()) {
                    return possibleQuery.get().list();
                }
            }
        }
        return val;
    }

    @Override
    public <T> Long getTotalRecords(Class<T> entityClass) {
        final Criteria sessionCriteria = session.createCriteria(entityClass);
        sessionCriteria.setProjection(Projections.rowCount());
        return (Long) sessionCriteria.uniqueResult();
    }

    @Override
    @Deprecated
    public <T> Collection filterCollection(Collection collection, Class<T> entityClass, Set<Predicate> predicates) {
        if ((collection instanceof AbstractPersistentCollection) && !predicates.isEmpty()) {
            // for PatchRequest use only inMemory tests since objects in the collection may be new and unsaved
            if (getRequestScope() instanceof PatchRequestScope) {
                return patchRequestFilterCollection(collection, entityClass, predicates);
            }

            String filterString = new HQLFilterOperation().applyAll(predicates);

            if (filterString.length() != 0) {
                Query query = session.createFilter(collection, filterString);

                for (Predicate predicate : predicates) {
                    if (predicate.getOperator().isParameterized()) {
                        String name = predicate.getNamedParameter();
                        query = query.setParameterList(name, predicate.getValues());
                    }
                }

                return query.list();
            }
        }

        return collection;
    }

    /**
     * for PatchRequest use only inMemory tests since objects in the collection may be new and unsaved
     * @param <T>         the type parameter
     * @param collection  the collection to filter
     * @param entityClass the class of the entities in the collection
     * @param filterExpression the filter expression
     * @return the filtered collection
     */
    protected <T> Collection patchRequestFilterCollection(
            Collection collection,
            Class<T> entityClass,
            FilterExpression filterExpression) {
        final EntityDictionary dictionary = getRequestScope().getDictionary();
        InMemoryFilterVisitor inMemoryFilterVisitor = new InMemoryFilterVisitor(dictionary);
        java.util.function.Predicate inMemoryFilterFn =
                filterExpression.accept(inMemoryFilterVisitor);
        return (Collection) collection.stream()
                .filter(e -> inMemoryFilterFn.test(e))
                .collect(Collectors.toList());
    }

    /**
     * for PatchRequest use only inMemory tests since objects in the collection may be new and unsaved
     * @param <T>         the type parameter
     * @param collection  the collection to filter
     * @param entityClass the class of the entities in the collection
     * @param predicates  the set of Predicate's to filter by
     * @return the filtered collection
     * @deprecated will be removed in Elide 3.0
     */
    @Deprecated
    protected <T> Collection patchRequestFilterCollection(Collection collection, Class<T> entityClass,
            Set<Predicate> predicates) {
        final EntityDictionary dictionary = getRequestScope().getDictionary();
        Set<java.util.function.Predicate> filterFns = new InMemoryFilterOperation(dictionary).applyAll(predicates);
        return (Collection) collection.stream()
                .filter(e -> filterFns.stream().allMatch(fn -> fn.test(e)))
                .collect(Collectors.toList());
    }

    @Override
    @Deprecated
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
        if (session.isOpen() && session.getTransaction().getStatus() == TransactionStatus.ACTIVE) {
            session.getTransaction().rollback();
            throw new IOException("Transaction not closed");
        }
    }

    @Override
    public User accessUser(Object opaqueUser) {
        return new User(opaqueUser);
    }
}
