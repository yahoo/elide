/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.modelconfig.model.Named;
import org.apache.commons.lang3.StringUtils;

/**
 * Metadata models that require a client filter.
 */
public interface RequiresFilter extends Named {
    Table getTable();

    String getRequiredFilter();

    default FilterExpression getRequiredFilter(EntityDictionary dictionary) {
        Type<?> cls = dictionary.getEntityClass(getTable().getName(), getTable().getVersion());
        RSQLFilterDialect filterDialect = new RSQLFilterDialect(dictionary);

        if (StringUtils.isNotEmpty(getRequiredFilter())) {
            try {
                return filterDialect.parseFilterExpression(getRequiredFilter(), cls, false, true);
            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }
}
