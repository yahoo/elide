/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter;

import com.google.common.base.Preconditions;

import java.util.List;

public class CaseSensitiveGenerators {

    public static class Infix implements FilterTranslator.JPQLPredicateGenerator {
        @Override
        public String generate(String columnAlias, List<FilterPredicate.FilterParameter> params) {
            Preconditions.checkState(params.size() == 1);
            String paramPlaceholder = params.get(0).getPlaceholder();
            assertValidValues(columnAlias, paramPlaceholder);
            return String.format("lower(%s) LIKE CONCAT('%%', lower(%s), '%%')", columnAlias, paramPlaceholder);
        }
    }
}
