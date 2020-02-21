/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.JoinFilterDialect;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.SubqueryFilterDialect;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.executors.ActivePermissionExecutor;
import com.yahoo.elide.security.executors.VerbosePermissionExecutor;
import com.yahoo.elide.utils.coerce.converters.EpochToDateConverter;
import com.yahoo.elide.utils.coerce.converters.ISO8601DateSerde;
import com.yahoo.elide.utils.coerce.converters.Serde;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;

/**
 * Builder for ElideSettings.
 */
public class ElideSettingsBuilder {
    private final DataStore dataStore;
    private AuditLogger auditLogger;
    private JsonApiMapper jsonApiMapper;
    private EntityDictionary entityDictionary = new EntityDictionary(new HashMap<>());
    private Function<RequestScope, PermissionExecutor> permissionExecutorFunction = ActivePermissionExecutor::new;
    private List<JoinFilterDialect> joinFilterDialects;
    private List<SubqueryFilterDialect> subqueryFilterDialects;
    private Map<Class, Serde> serdes;
    private int defaultMaxPageSize = PaginationImpl.MAX_PAGE_LIMIT;
    private int defaultPageSize = PaginationImpl.DEFAULT_PAGE_LIMIT;
    private int updateStatusCode;

    /**
     * A new builder used to generate Elide instances. Instantiates an {@link EntityDictionary} without
     * providing a mapping of security checks and uses the provided {@link Slf4jLogger} for audit.
     *
     * @param dataStore the datastore used to communicate with the persistence layer
     */
    public ElideSettingsBuilder(DataStore dataStore) {
        this.dataStore = dataStore;
        this.auditLogger = new Slf4jLogger();
        this.jsonApiMapper = new JsonApiMapper(entityDictionary);
        this.joinFilterDialects = new ArrayList<>();
        this.subqueryFilterDialects = new ArrayList<>();
        updateStatusCode = HttpStatus.SC_NO_CONTENT;
        this.serdes = new HashMap<>();

        //By default, Elide supports epoch based dates.
        this.withEpochDates();
    }

    public ElideSettings build() {
        if (joinFilterDialects.isEmpty()) {
            joinFilterDialects.add(new DefaultFilterDialect(entityDictionary));
            joinFilterDialects.add(new RSQLFilterDialect(entityDictionary));
        }

        if (subqueryFilterDialects.isEmpty()) {
            subqueryFilterDialects.add(new DefaultFilterDialect(entityDictionary));
            subqueryFilterDialects.add(new RSQLFilterDialect(entityDictionary));
        }

        return new ElideSettings(
                auditLogger,
                dataStore,
                entityDictionary,
                jsonApiMapper,
                permissionExecutorFunction,
                joinFilterDialects,
                subqueryFilterDialects,
                defaultMaxPageSize,
                defaultPageSize,
                updateStatusCode,
                serdes);
    }

    public ElideSettingsBuilder withAuditLogger(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
        return this;
    }

    public ElideSettingsBuilder withEntityDictionary(EntityDictionary entityDictionary) {
        this.entityDictionary = entityDictionary;
        return this;
    }

    public ElideSettingsBuilder withJsonApiMapper(JsonApiMapper jsonApiMapper) {
        this.jsonApiMapper = jsonApiMapper;
        return this;
    }

    public ElideSettingsBuilder withJoinFilterDialect(JoinFilterDialect dialect) {
        joinFilterDialects.add(dialect);
        return this;
    }

    public ElideSettingsBuilder withSubqueryFilterDialect(SubqueryFilterDialect dialect) {
        subqueryFilterDialects.add(dialect);
        return this;
    }

    public ElideSettingsBuilder withDefaultMaxPageSize(int maxPageSize) {
        defaultMaxPageSize = maxPageSize;
        return this;
    }

    public ElideSettingsBuilder withDefaultPageSize(int pageSize) {
        defaultPageSize = pageSize;
        return this;
    }

    public ElideSettingsBuilder withUpdate200Status() {
        updateStatusCode = HttpStatus.SC_OK;
        return this;
    }

    public ElideSettingsBuilder withUpdate204Status() {
        updateStatusCode = HttpStatus.SC_NO_CONTENT;
        return this;
    }

    public ElideSettingsBuilder withVerboseErrors() {
        permissionExecutorFunction = VerbosePermissionExecutor::new;
        return this;
    }

    public ElideSettingsBuilder withISO8601Dates(String dateFormat, TimeZone tz) {
        serdes.put(Date.class, new ISO8601DateSerde(dateFormat, tz));
        serdes.put(java.sql.Date.class, new ISO8601DateSerde(dateFormat, tz, java.sql.Date.class));
        serdes.put(java.sql.Time.class, new ISO8601DateSerde(dateFormat, tz, java.sql.Time.class));
        serdes.put(java.sql.Timestamp.class, new ISO8601DateSerde(dateFormat, tz, java.sql.Timestamp.class));
        return this;
    }

    public ElideSettingsBuilder withEpochDates() {
        serdes.put(Date.class, new EpochToDateConverter<Date>(Date.class));
        serdes.put(java.sql.Date.class, new EpochToDateConverter<java.sql.Date>(java.sql.Date.class));
        serdes.put(java.sql.Time.class, new EpochToDateConverter<java.sql.Time>(java.sql.Time.class));
        serdes.put(java.sql.Timestamp.class, new EpochToDateConverter<java.sql.Timestamp>(java.sql.Timestamp.class));
        return this;
    }
}
