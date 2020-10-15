package com.yahoo.elide.core.filter.dialect;

import com.yahoo.elide.core.filter.expression.FilterExpression;

import java.util.Map;

public interface FilterDialect {
    public FilterExpression parse(Class<?> entityClass,
                                  Map<String, String> aliasMap,
                                  String filterText) throws ParseException;
}
