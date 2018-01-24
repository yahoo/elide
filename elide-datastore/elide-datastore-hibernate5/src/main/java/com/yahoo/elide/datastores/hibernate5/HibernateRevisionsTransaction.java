package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.EnverseFilterOperation;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.hibernate.hql.RootCollectionFetchQueryBuilder;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.hibernate5.porting.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;

import java.io.Serializable;
import java.util.Collections;
import java.util.Optional;

@Slf4j
public class HibernateRevisionsTransaction extends HibernateTransaction {

    private AuditReader auditReader;

    public HibernateRevisionsTransaction(AuditReader auditReader, Session session) {
        super(session, false, ScrollMode.SCROLL_INSENSITIVE);
        session.beginTransaction();
        this.auditReader = auditReader;
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
        log.debug(String.format("Revision: %d", scope.getHistoricalRevision()));
        if (!isHistory(scope)) {
            return super.loadObject(entityClass, id, filterExpression, scope);
        }
        try {
            EntityDictionary dictionary = scope.getDictionary();
            Class<?> idType = dictionary.getIdType(entityClass);
            String idField = dictionary.getIdFieldName(entityClass);

            //Construct a predicate that selects an individual element of the relationship's parent (Author.id = 3).
            FilterPredicate idExpression;
            Path.PathElement idPath = new Path.PathElement(entityClass, idType, idField);
            if (id != null) {
                idExpression = new FilterPredicate(idPath, Operator.IN, Collections.singletonList(id));
            } else {
                idExpression = new FilterPredicate(idPath, Operator.FALSE, Collections.emptyList());
            }

            FilterExpression joinedExpression = filterExpression
                    .map(fe -> (FilterExpression) new AndFilterExpression(fe, idExpression))
                    .orElse(idExpression);

            EnverseFilterOperation operation = new EnverseFilterOperation(scope.getDictionary());
            AuditCriterion criteria = operation.apply(joinedExpression);
            return auditReader.createQuery().forEntitiesAtRevision(entityClass, getRevision(scope)).add(criteria).getSingleResult();
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
        log.debug(String.format("Revision: %d", getRevision(scope)));
        if (!isHistory(scope)) {
            return super.loadObjects(entityClass, filterExpression, sorting, pagination, scope);
        }

        EnverseFilterOperation operation = new EnverseFilterOperation(scope.getDictionary());
        if (filterExpression.isPresent()) {
            AuditCriterion criteria = operation.apply(filterExpression.get());
            return auditReader.createQuery().forEntitiesAtRevision(entityClass, getRevision(scope)).add(criteria).getResultList();
        } else {
            return auditReader.createQuery().forEntitiesAtRevision(entityClass, getRevision(scope)).getResultList();
        }
    }

    private boolean isHistory(RequestScope scope) {
        return scope.getHistoricalRevision() != null || scope.getHistoricalDatestamp() != null;
    }

    private Integer getRevision(RequestScope scope) {
        if (scope.getHistoricalRevision() != null) {
            return scope.getHistoricalRevision().intValue();
        } else {
            Query query = this.session.createSQLQuery("SELECT MAX(REV) from REVINFO WHERE REVTSTMP <= :timestamp");
            query.setParameter("timestamp", scope.getHistoricalDatestamp());
            log.debug(String.format("Query: %s", query.toString()));
            log.debug(String.format("ts: %d", scope.getHistoricalDatestamp()));
            return (Integer) query.uniqueResult();
        }
    }
}
