package com.yahoo.elide.dbmanagers.hibernate5;

import com.yahoo.elide.core.DatabaseTransaction;
import com.yahoo.elide.core.FilterScope;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.CriteriaCheck;
import com.yahoo.elide.security.User;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.ScrollMode;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Hibernate Transaction implementation
 */
public class HibernateTransaction implements DatabaseTransaction {

    private HibernateManager hibernateManager;
    private Transaction transaction;
    private LinkedHashSet<Runnable> deferredTasks = new LinkedHashSet<>();

    /**
     * Instantiates a new Hibernate transaction.
     *
     * @param hibernateManager the hibernate manager
     * @param transaction      the transaction
     */
    public HibernateTransaction(HibernateManager hibernateManager, Transaction transaction) {
        this.hibernateManager = hibernateManager;
        this.transaction = transaction;
    }

    @Override
    public void delete(Object object) {
        deferredTasks.add(() -> hibernateManager.getSession().delete(object));
    }

    @Override
    public void save(Object object) {
        deferredTasks.add(() -> hibernateManager.getSession().saveOrUpdate(object));
    }

    @Override
    public void flush() {
        try {
            for (Runnable task : deferredTasks) {
                task.run();
            }
            deferredTasks.clear();
            hibernateManager.getSession().flush();
        } catch (HibernateException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public void commit() {
        try {
            this.flush();
            this.transaction.commit();
        } catch (HibernateException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public <T> T createObject(Class<T> entityClass) {
        try {
            T object = entityClass.newInstance();
            deferredTasks.add(() -> hibernateManager.getSession().persist(object));
            return object;
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public <T> T loadObject(Class<T> loadClass, String id) {
        @SuppressWarnings("unchecked")
        T record = hibernateManager.getSession().load(loadClass, Long.valueOf(id));
        try {
            Hibernate.initialize(record);
        } catch (ObjectNotFoundException e) {
            return null;
        }
        return record;
    }

    @Override
    public <T> Iterable<T> loadObjects(Class<T> loadClass) {
        @SuppressWarnings("unchecked")
        Iterable<T> list = new HibernateManager.ScrollableIterator(hibernateManager.getSession().createCriteria(loadClass)
                .scroll(ScrollMode.FORWARD_ONLY));
        return list;
    }

    @Override
    public <T> Iterable<T> loadObjects(Class<T> loadClass, FilterScope<T> filterScope) {
        Criterion criterion = buildCriterion(filterScope);

        // if no criterion then return all objects
        if (criterion == null) {
            return loadObjects(loadClass);
        }

        @SuppressWarnings("unchecked")
        Iterable<T> list = new HibernateManager.ScrollableIterator(hibernateManager.getSession().createCriteria(loadClass)
                .add(criterion)
                .scroll(ScrollMode.FORWARD_ONLY));
        return list;
    }

    /**
     * builds criterion if all checks implement CriteriaCheck
     *
     * @param <T>         Filter type
     * @param filterScope Filter Scope
     * @return the criterion
     */
    public <T> Criterion buildCriterion(FilterScope<T> filterScope) {
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
            } else  if (compositeCriterion == null) {
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
    public void close() throws IOException {
        if (this.transaction.getStatus() == TransactionStatus.ACTIVE) {
            transaction.rollback();
            throw new IOException("Transaction not closed");
        }
    }

    @Override
    public User accessUser(Object opaqueUser) {
        return new User(opaqueUser);
    }
}
