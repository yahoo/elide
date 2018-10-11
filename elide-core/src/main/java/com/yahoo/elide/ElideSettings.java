/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.JoinFilterDialect;
import com.yahoo.elide.core.filter.dialect.SubqueryFilterDialect;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.utils.coerce.converters.Serde;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Object containing general Elide settings passed to RequestScope.
 */
@AllArgsConstructor
public class ElideSettings {
    @Getter private final AuditLogger auditLogger;
    @Getter private final DataStore dataStore;
    @Getter private final EntityDictionary dictionary;
    @Getter private final JsonApiMapper mapper;
    @Getter private final Function<RequestScope, PermissionExecutor> permissionExecutor;
    @Getter private final List<JoinFilterDialect> joinFilterDialects;
    @Getter private final List<SubqueryFilterDialect> subqueryFilterDialects;
    @Getter private final int defaultMaxPageSize;
    @Getter private final int defaultPageSize;
    @Getter private final boolean useFilterExpressions;
    @Getter private final int updateStatusCode;
    @Getter private final boolean returnErrorObjects;
    @Getter private final Map<Class, Serde> serdes;
}
