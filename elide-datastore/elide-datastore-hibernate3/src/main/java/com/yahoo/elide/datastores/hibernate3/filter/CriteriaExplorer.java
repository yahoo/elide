/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3.filter;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.Predicate;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;

import java.util.Map;
import java.util.Set;

/**
 * Explorer intended to construct criteria from filtering for constraining object loading.
 */
public class CriteriaExplorer {
    private final Criterion rootCriterion;
    private final CriterionFilterOperation filterOperation;
    private final RequestScope requestScope;
    private final EntityDictionary dictionary;
    private final Class<?> loadClass;

    /**
     * Constructor.
     *
     * @param loadClass Root class being loaded
     * @param requestScope Request scope
     * @param existing Pre-existing criterion related to loadClass
     */
    public CriteriaExplorer(Class<?> loadClass, RequestScope requestScope, Criterion existing) {
        this.filterOperation = new CriterionFilterOperation();
        this.requestScope = requestScope;
        this.dictionary = requestScope.getDictionary();
        this.rootCriterion = buildRootCriterion(loadClass, existing);
        this.loadClass = loadClass;
    }

    /**
     * Build a set of criteria from sessionCriteria.
     *
     * NOTE: It is assumed that sessionCriteria is a criteria corresponding to the "loadClass"
     *       that this was instantiated with.
     *
     * @param sessionCriteria Session criteria to update
     * @param session Session to create criteria on
     */
    public void buildCriteria(final Criteria sessionCriteria, final Session session) {
        if (rootCriterion != null) {
            sessionCriteria.add(rootCriterion);
        }

        for (Map.Entry<String, Set<Predicate>> entry : requestScope.getPredicates().entrySet()) {
            String criteriaPath = entry.getKey();
            Set<Predicate> predicates = entry.getValue();

            String[] objects = criteriaPath.split("\\.");

            Criteria criteria;
            Class filterClass = dictionary.getEntityClass(objects[0]);
            if (loadClass.equals(filterClass)) {
                criteria = sessionCriteria;
            } else {
                criteria = session.createCriteria(filterClass);
            }

            for (int i = 1 ; i < objects.length ; ++i) {
                criteria = criteria.createCriteria(objects[i]);
            }
            criteria.add(filterOperation.applyAll(predicates));
        }
    }

    /**
     * Build root criteria object.
     *
     * @param loadClass Root class being loaded
     * @param existing Pre-existing criterion related to loadClass
     * @return Criterion for root class
     */
    private Criterion buildRootCriterion(final Class<?> loadClass, final Criterion existing) {
        String type = dictionary.getJsonAliasFor(loadClass);
        return CriterionFilterOperation.andWithNull(existing,
                filterOperation.applyAll(requestScope.getPredicatesOfType(type)));
    }
}
