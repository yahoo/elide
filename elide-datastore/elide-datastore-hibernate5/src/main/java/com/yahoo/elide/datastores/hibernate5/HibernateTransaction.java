/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.InMemoryFilterVisitor;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.hibernate5.filter.CriterionFilterOperation;
import com.yahoo.elide.extensions.PatchRequestScope;
import com.yahoo.elide.security.User;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
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
            FlushMode flushMode = session.getFlushMode();
            if (flushMode != FlushMode.COMMIT && flushMode != FlushMode.MANUAL) {
                session.flush();
            }
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
     * @param scope Request scope associated with specific request
     */
    @Override
    public Object loadObject(Class<?> entityClass,
                             Serializable id,
                             Optional<FilterExpression> filterExpression,
                             RequestScope scope) {

        try {
            Criteria criteria = session.createCriteria(entityClass).add(Restrictions.idEq(id));
            if (filterExpression.isPresent()) {
                CriterionFilterOperation filterOpn = buildCriterionFilterOperation(criteria);
                criteria = filterOpn.apply(filterExpression.get());
            }
            return criteria.uniqueResult();
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    /**
     * Build the CriterionFilterOperation for provided criteria.
     *
     * @param criteria the criteria
     * @return the CriterionFilterOperation
     */
    protected CriterionFilterOperation buildCriterionFilterOperation(Criteria criteria) {
        return new CriterionFilterOperation(criteria);
    }

    @Override
    public Iterable<Object> loadObjects(
            Class<?> entityClass,
            Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope) {

        final EntityDictionary dictionary = scope.getDictionary();
        pagination.ifPresent(p -> {
            if (p.isGenerateTotals()) {
                p.setPageTotals(getTotalRecords(entityClass, filterExpression));
            }
        });

        Criteria criteria = session.createCriteria(entityClass);

        if (filterExpression.isPresent()) {
            CriterionFilterOperation filterOpn = buildCriterionFilterOperation(criteria);
            criteria = filterOpn.apply(filterExpression.get());
        }

        Set<Order> validatedSortingRules = null;
        if (sorting.isPresent()) {
            if (!sorting.get().isDefaultInstance()) {
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
    }


    /**
     * Generates the Hibernate ScrollableIterator for Hibernate Query.
     *
     * @param loadClass The hibernate class to build the query off of.
     * @param criteria The criteria to use for filters
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
            Pagination paginationData = pagination.get();
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

        EntityDictionary dictionary = scope.getDictionary();
        Object val = com.yahoo.elide.core.PersistentResource.getValue(entity, relationName, scope);
        if (val instanceof Collection) {
            Collection filteredVal = (Collection) val;
            if (filteredVal instanceof AbstractPersistentCollection) {
                if (scope instanceof PatchRequestScope && filterExpression.isPresent()) {
                    Class relationClass = dictionary.getType(entity, relationName);
                    return patchRequestFilterCollection(filteredVal,
                            relationClass, filterExpression.get(), (PatchRequestScope) scope);
                }

                @SuppressWarnings("unchecked")
                Class<?> relationClass = dictionary.getParameterizedType(entity, relationName);

                pagination.ifPresent(p -> {
                    if (p.isGenerateTotals()) {
                        p.setPageTotals(getTotalRecords(entity, filterExpression, relationName, dictionary));
                    }
                });

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

    /**
     * Returns the total record count for a root entity and an optional filter expression.
     * @param entityClass The entity type to count
     * @param filterExpression optional security and request filters
     * @param <T> The type of entity
     * @return The total row count.
     */
    private <T> Long getTotalRecords(Class<T> entityClass,
                                    Optional<FilterExpression> filterExpression) {
        String queryString = "SELECT COUNT(*) FROM {parentType} {parentType}";
        queryString = queryString.replaceAll("\\{parentType\\}", entityClass.getSimpleName());

        Query query;
        if (filterExpression.isPresent()) {
            query = populateWhereClause(queryString, filterExpression.get());
        } else {
            query = session.createQuery(queryString);
        }
        return (Long) query.uniqueResult();
    }

    /**
     * Returns the total record count for a entity relationship
     * @param entity The entity which owns the relationship
     * @param filterExpression optional security and request filters
     * @param <T> The type of entity
     * @return The total row count.
     */
    private <T> Long getTotalRecords(Object entity,
                                     Optional<FilterExpression> filterExpression,
                                     String relation,
                                     EntityDictionary dictionary) {
        Class<?> entityType = entity.getClass();

        Class<?> idType = dictionary.getIdType(entityType);
        Object idVal = CoerceUtil.coerce(dictionary.getId(entity), idType);
        String idField = dictionary.getIdFieldName(entityType);

        String parentTypeAlias = getRandomAlias(entityType);

        FilterPredicate idExpression = new FilterPredicate(
                new FilterPredicate.PathElement(
                        entityType,
                        entityType.getSimpleName(),
                        idType,
                        idField),
                Operator.IN,
                Collections.singletonList(idVal));

        idExpression.setAlias(parentTypeAlias);

        FilterExpression joinedExpression = idExpression;
        if (filterExpression.isPresent()) {
            joinedExpression = new AndFilterExpression(filterExpression.get(), idExpression);
        }

        Class<?> relationClass = dictionary.getParameterizedType(entityType, relation);
        String queryString =
                "SELECT COUNT(*) FROM {parentType} {parentTypeAlias} join {parentTypeAlias}.{relation} {relationType}";
        queryString = queryString.replaceAll("\\{parentType\\}", entityType.getSimpleName());
        queryString = queryString.replaceAll("\\{parentTypeAlias\\}", parentTypeAlias);
        queryString = queryString.replaceAll("\\{relation\\}", relation);
        queryString = queryString.replaceAll("\\{relationType\\}", relationClass.getSimpleName());

        Query query = populateWhereClause(queryString, joinedExpression);
        return (Long) query.uniqueResult();
    }

    /**
     * Builds a Hibernate query from a HQL fragment (containing SELECT & FROM) and a filter expression.
     * @param hqlQuery The HQL fragment
     * @param expression the filter expression to expand into a WHERE clause.
     * @return an executable query.
     */
    private Query populateWhereClause(String hqlQuery, FilterExpression expression) {
        String completeQuery = hqlQuery + " " + new HQLFilterOperation().apply(expression, true);

        Query query = session.createQuery(completeQuery);

        /* Extract the predicates from the expression */
        PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();
        Set<FilterPredicate> predicates = expression.accept(visitor);

        /* Populate query parameters from each predicate*/
        for (FilterPredicate filterPredicate : predicates) {
            if (filterPredicate.getOperator().isParameterized()) {
                String name = filterPredicate.getParameterName();
                if(filterPredicate.getOperator() == Operator.INFIX || filterPredicate.getOperator() == Operator.INFIX_CASE_INSENSITIVE ||
                        filterPredicate.getOperator() == Operator.PREFIX || filterPredicate.getOperator() == Operator.PREFIX_CASE_INSENSITIVE ||
                        filterPredicate.getOperator() == Operator.POSTFIX || filterPredicate.getOperator() == Operator.POSTFIX_CASE_INSENSITIVE){
                    query = query.setParameter(name, filterPredicate.getStringValueEscaped("%", "\\"));
                }
                else{
                    query = query.setParameterList(name, filterPredicate.getValues());
                }
            }
        }

        return query;
    }

    /**
     * Use only inMemory tests during PatchRequest since objects in the collection may be new and unsaved.
     *
     * @param <T> the type parameter
     * @param collection the collection to filter
     * @param entityClass the class of the entities in the collection
     * @param filterExpression the filter expression
     * @param requestScope the request scope
     * @return the filtered collection
     */
    protected <T> Collection patchRequestFilterCollection(
            Collection collection,
            Class<T> entityClass,
            FilterExpression filterExpression,
            com.yahoo.elide.core.RequestScope requestScope) {
        InMemoryFilterVisitor inMemoryFilterVisitor = new InMemoryFilterVisitor(requestScope);
        Predicate inMemoryFilterFn = filterExpression.accept(inMemoryFilterVisitor);
        return (Collection) collection.stream()
                .filter(e -> inMemoryFilterFn.test(e))
                .collect(Collectors.toList());
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

    private static String getRandomAlias(Class<?> entityType) {
        return entityType.getSimpleName() + ThreadLocalRandom.current().nextInt(1, 1000);
    }
}
