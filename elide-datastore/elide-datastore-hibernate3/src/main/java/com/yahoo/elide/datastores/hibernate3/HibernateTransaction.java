/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.FilterScope;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.HQLFilterOperation;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.datastores.hibernate3.filter.CriterionFilterOperation;
import com.yahoo.elide.datastores.hibernate3.security.CriteriaCheck;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.User;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.collection.AbstractPersistentCollection;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Hibernate Transaction implementation.
 */
public class HibernateTransaction implements DataStoreTransaction {
    private final Session session;
    private final LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();
    private final HQLFilterOperation hqlFilterOperation = new HQLFilterOperation();
    private final CriterionFilterOperation criterionFilterOperation = new CriterionFilterOperation();

    /**
     * Instantiates a new Hibernate transaction.
     *
     * @param session the session
     */
    public HibernateTransaction(Session session) {
        this.session = session;
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
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public <T> T loadObject(Class<T> loadClass, Serializable id) {
        try {
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
        @SuppressWarnings("unchecked")
        Iterable<T> list = new ScrollableIterator(session.createCriteria(loadClass)
                .scroll(ScrollMode.FORWARD_ONLY));
        return list;
    }

    @Override
    public <T> Iterable<T> loadObjects(Class<T> loadClass, FilterScope<T> filterScope) {
        Criterion criterion = buildCheckCriterion(filterScope);

        String type = filterScope.getRequestScope().getDictionary().getBinding(loadClass);
        Set<Predicate> filteredPredicates = filterScope.getRequestScope().getPredicatesOfType(type);
        criterion = CriterionFilterOperation.andWithNull(criterion,
                criterionFilterOperation.applyAll(filteredPredicates));

        // if no criterion then return all objects
        if (criterion == null) {
            return loadObjects(loadClass);
        }

        @SuppressWarnings("unchecked")
        Iterable<T> list = new ScrollableIterator(session.createCriteria(loadClass)
                .add(criterion)
                .scroll(ScrollMode.FORWARD_ONLY));
        return list;
    }

    /**
     * builds criterion if all checks implement CriteriaCheck.
     *
     * @param filterScope the filterScope
     * @return the criterion
     */
    public <T> Criterion buildCheckCriterion(FilterScope<T> filterScope) {
        Criterion compositeCriterion = null;
        List<Check<T>> checks = filterScope.getChecks();
        RequestScope requestScope = filterScope.getRequestScope();
        for (Check check : checks) {
            Criterion criterion;
            if (check instanceof CriteriaCheck) {
                criterion = ((CriteriaCheck) check).getCriterion(requestScope);
            } else {
                criterion = null;
            }

            // if no criterion, examine userPermission and ANY state
            if (criterion == null) {
                switch (filterScope.getRequestScope().getUser().checkUserPermission(check)) {
                    // ALLOW and ALL try more criteria
                    case ALLOW:
                        if (!filterScope.isAny()) {
                            continue;
                        }
                        break;

                    // DENY and ANY check try more criteria
                    case DENY:
                        if (filterScope.isAny()) {
                            continue;
                        }
                        break;
                }

                // Otherwise no criteria filtering possible
                return null;
            } else if (compositeCriterion == null) {
                compositeCriterion = criterion;
            } else if (filterScope.isAny()) {
                compositeCriterion = Restrictions.or(compositeCriterion, criterion);
            } else {
                compositeCriterion = Restrictions.and(compositeCriterion, criterion);
            }
        }

        return compositeCriterion;
    }

    @Override
    public <T> Collection filterCollection(Collection collection, Class<T> entityClass, Set<Predicate> predicates) {
        if ((collection instanceof AbstractPersistentCollection) && !predicates.isEmpty()) {
            String filterString = hqlFilterOperation.applyAll(predicates);

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
}
