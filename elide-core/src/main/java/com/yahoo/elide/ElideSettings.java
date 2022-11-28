/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.filter.dialect.graphql.FilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.JoinFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.SubqueryFilterDialect;
import com.yahoo.elide.core.security.PermissionExecutor;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.links.JSONApiLinks;
import com.yahoo.elide.utils.HeaderUtils;

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
    @Getter private final ErrorMapper errorMapper;
    @Getter private final Function<RequestScope, PermissionExecutor> permissionExecutor;
    @Getter private final List<JoinFilterDialect> joinFilterDialects;
    @Getter private final List<SubqueryFilterDialect> subqueryFilterDialects;
    @Getter private final FilterDialect graphqlDialect;
    @Getter private final JSONApiLinks jsonApiLinks;
    @Getter private final HeaderUtils.HeaderProcessor headerProcessor;
    @Getter private final int defaultMaxPageSize;
    @Getter private final int defaultPageSize;
    @Getter private final int updateStatusCode;
    @Getter private final Map<Class, Serde> serdes;
    @Getter private final boolean enableJsonLinks;
    @Getter private final boolean strictQueryParams;
    @Getter private final boolean enableGraphQLFederation;
    @Getter private final String baseUrl;
    @Getter private final String jsonApiPath;
    @Getter private final String graphQLApiPath;
    @Getter private final String exportApiPath;
}
