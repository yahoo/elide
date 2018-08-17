package com.yahoo.elide.core.hibernate.enverse;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;

import java.util.Optional;

public class RootCollectionFetchRevisionQueryBuilder {

    private Class<?> entityClass;
    private EntityDictionary dictionary;
    private FilterExpression filterExpression;

    public RootCollectionFetchRevisionQueryBuilder(Class<?> entityClass,
                                                   EntityDictionary dictionary) {
        this.entityClass = entityClass;
        this.dictionary = dictionary;
    }

    public RootCollectionFetchRevisionQueryBuilder withPossibleFilterExpression(Optional<FilterExpression> filterExpression) {
        this.filterExpression = filterExpression.get();
        return this;
    }




}
