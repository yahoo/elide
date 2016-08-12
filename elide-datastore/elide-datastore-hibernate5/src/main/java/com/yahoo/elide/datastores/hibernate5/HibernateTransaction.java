/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.hibernate5.filter.CriterionFilterOperation;
import com.yahoo.elide.security.User;

import org.antlr.v4.runtime.tree.ParseTree;
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
public class HibernateTransaction implements DataStoreTransaction {
    private static final Function<Criterion, Criterion> NOT = Restrictions::not;
    private static final BiFunction<Criterion, Criterion, Criterion> AND = Restrictions::and;
    private static final BiFunction<Criterion, Criterion, Criterion> OR = Restrictions::or;

    private final Session session;
    private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();
    private final boolean isScrollEnabled;
    private final ScrollMode scrollMode;

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
        try {
            com.yahoo.elide.core.RequestScope requestScope = (com.yahoo.elide.core.RequestScope) scope;
            ParseTree permissions = requestScope.getDictionary()
                    .getPermissionsForClass(entityClass, ReadPermission.class);
            Criterion securityCriterion = requestScope.getPermissionExecutor()
                    .getCriterion(permissions, NOT, AND, OR);

            Criteria criteria = session.createCriteria(entityClass);
            if (securityCriterion != null) {
                criteria.add(securityCriterion);
            }

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
                    pagination);
        } catch (ClassCastException e) {
            throw new ClassCastException("Fail casting requestscope");
        }
    }


    /**
     * Generates the Hibernate ScrollableIterator for Hibernate Query.
     * @param loadClass The hibernate class to build the query off of.
     * @param criteria Filtering criteria
     * @param sortingRules The possibly empty sorting rules.
     * @param pagination The Optional pagination object.
     * @return The Iterable for Hibernate.
     */
    public Iterable loadObjects(final Class<?> loadClass, final Criteria criteria,
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
    public Object getRelation(
            DataStoreTransaction relationTx,
            Object entity,
            String relationName,
            Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope) {
        try {
            com.yahoo.elide.core.RequestScope requestScope = (com.yahoo.elide.core.RequestScope) scope;
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
        } catch (ClassCastException e) {
            throw new ClassCastException("Fail casting requestscope");
        }
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
            String filterString = new HQLFilterOperation().applyAll(predicates);

            if (filterString.length() != 0) {
                Query query = session.createFilter(collection, filterString);

                for (Predicate predicate : predicates) {
                    if (predicate.getOperator().isParameterized()) {
                        query = query.setParameterList(predicate.getField(), predicate.getValues());
                    }
                }

                return query.list();
            }
        }

        return collection;
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
