/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.security.PermissionExecutor;
import com.yahoo.elide.core.security.executors.ActivePermissionExecutor;
import com.yahoo.elide.core.security.executors.VerbosePermissionExecutor;
import com.yahoo.elide.utils.HeaderUtils;
import com.yahoo.elide.utils.HeaderUtils.HeaderProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contains the Elide settings passed to RequestScope.
 *
 * Use the static factory {@link #builder()} method to prepare an instance.
 */
@Getter
public class ElideSettings {
    private final AuditLogger auditLogger;
    private final DataStore dataStore;
    private final EntityDictionary entityDictionary;
    private final ObjectMapper objectMapper;
    private final ErrorMapper errorMapper;
    private final Function<RequestScope, PermissionExecutor> permissionExecutor;
    private final HeaderUtils.HeaderProcessor headerProcessor;
    private final int defaultMaxPageSize;
    private final int defaultPageSize;
    private final Serdes serdes;
    private final String baseUrl;
    private final Map<Class<? extends Settings>, Settings> settings;

    public ElideSettings(AuditLogger auditLogger, DataStore dataStore, EntityDictionary entityDictionary,
            ObjectMapper objectMapper, ErrorMapper errorMapper,
            Function<RequestScope, PermissionExecutor> permissionExecutor, HeaderProcessor headerProcessor,
            int defaultMaxPageSize, int defaultPageSize, Serdes serdes, String baseUrl,
            Map<Class<? extends Settings>, Settings> settings) {
        super();
        this.auditLogger = auditLogger;
        this.dataStore = dataStore;
        this.entityDictionary = entityDictionary;
        this.objectMapper = objectMapper;
        this.errorMapper = errorMapper;
        this.permissionExecutor = permissionExecutor;
        this.headerProcessor = headerProcessor;
        this.defaultMaxPageSize = defaultMaxPageSize;
        this.defaultPageSize = defaultPageSize;
        this.serdes = serdes;
        this.baseUrl = baseUrl;
        this.settings = settings;
    }

    public <T extends Settings> T getSettings(Class<T> clazz) {
        return clazz.cast(this.settings.get(clazz));
    }

    public static ElideSettingsBuilder builder() {
        return new ElideSettingsBuilder();
    }

    public static class ElideSettingsBuilder extends ElideSettingsBuilderSupport<ElideSettingsBuilder> {

        @Override
        public ElideSettingsBuilder self() {
            return this;
        }

        public ElideSettings build() {
            Map<Class<? extends Settings>, Settings> settings = new LinkedHashMap<>();
            this.settings.values().forEach(value -> {
                Settings result = value.build();
                settings.put(result.getClass(), result);
            });
            return new ElideSettings(this.auditLogger, this.dataStore, this.entityDictionary, this.objectMapper,
                    this.errorMapper, this.permissionExecutor, this.headerProcessor, this.defaultMaxPageSize,
                    this.defaultPageSize, this.serdes.build(), this.baseUrl, settings);
        }
    }

    public abstract static class ElideSettingsBuilderSupport<S> {
        protected Serdes.SerdesBuilder serdes = Serdes.builder();
        protected String baseUrl = "";
        protected AuditLogger auditLogger = new Slf4jLogger();
        protected HeaderUtils.HeaderProcessor headerProcessor = HeaderUtils::lowercaseAndRemoveAuthHeaders;
        protected ObjectMapper objectMapper = new ObjectMapper();
        protected int defaultMaxPageSize = Pagination.MAX_PAGE_LIMIT;
        protected int defaultPageSize = Pagination.DEFAULT_PAGE_LIMIT;
        protected Function<RequestScope, PermissionExecutor> permissionExecutor = ActivePermissionExecutor::new;
        protected Map<Class<?>, Settings.SettingsBuilder> settings = new LinkedHashMap<>();
        protected DataStore dataStore;
        protected EntityDictionary entityDictionary;
        protected ErrorMapper errorMapper;

        protected ElideSettingsBuilderSupport() {
            // By default, Elide supports epoch based dates.
            // This can be cleared by serdes -> serdes.clear()
            this.serdes.withDefaults().withEpochDates().build();
        }

        public <T extends Settings.SettingsBuilder> T getSettings(Class<T> clazz) {
            return clazz.cast(this.settings.get(clazz));
        }

        public S serdes(Consumer<Serdes.SerdesBuilder> serdes) {
            serdes.accept(this.serdes);
            return self();
        }

        public S settings(Settings.SettingsBuilder... settings) {
            Arrays.asList(settings).stream().forEach(item -> this.settings.put(item.getClass(), item));
            return self();
        }

        public S settings(Consumer<Map<Class<?>, Settings.SettingsBuilder>> settings) {
            settings.accept(this.settings);
            return self();
        }

        public S verboseErrors(boolean verboseErrors) {
            if (verboseErrors) {
                this.permissionExecutor = VerbosePermissionExecutor::new;
            } else {
                this.permissionExecutor = ActivePermissionExecutor::new;
            }
            return self();
        }

        public S baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return self();
        }

        public S auditLogger(AuditLogger auditLogger) {
            this.auditLogger = auditLogger;
            return self();
        }

        public S headerProcessor(HeaderUtils.HeaderProcessor headerProcessor) {
            this.headerProcessor = headerProcessor;
            return self();
        }

        public S objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return self();
        }

        public S defaultMaxPageSize(int defaultMaxPageSize) {
            this.defaultMaxPageSize = defaultMaxPageSize;
            return self();
        }

        public S defaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
            return self();
        }

        public S permissionExecutor(Function<RequestScope, PermissionExecutor> permissionExecutor) {
            this.permissionExecutor = permissionExecutor;
            return self();
        }

        public S dataStore(DataStore dataStore) {
            this.dataStore = dataStore;
            return self();
        }

        public S entityDictionary(EntityDictionary entityDictionary) {
            this.entityDictionary = entityDictionary;
            return self();
        }

        public S errorMapper(ErrorMapper errorMapper) {
            this.errorMapper = errorMapper;
            return self();
        }

        public abstract S self();
    }
}
