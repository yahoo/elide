/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import com.paiondata.elide.Serdes.SerdesBuilder;
import com.paiondata.elide.Settings.SettingsBuilder;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.audit.AuditLogger;
import com.paiondata.elide.core.audit.Slf4jLogger;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.ExceptionMappers;
import com.paiondata.elide.core.request.Pagination;
import com.paiondata.elide.core.security.PermissionExecutor;
import com.paiondata.elide.core.security.executors.ActivePermissionExecutor;
import com.paiondata.elide.utils.HeaderProcessor;
import com.paiondata.elide.utils.Headers;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contains the Elide settings passed to {@link RequestScope}.
 * <p>
 * Use the static factory {@link #builder()} method to prepare an instance.
 */
@Getter
public class ElideSettings {
    private final AuditLogger auditLogger;
    private final DataStore dataStore;
    private final EntityDictionary entityDictionary;
    private final ObjectMapper objectMapper;
    private final Function<RequestScope, PermissionExecutor> permissionExecutor;
    private final HeaderProcessor headerProcessor;
    private final int maxPageSize;
    private final int defaultPageSize;
    private final Serdes serdes;
    private final String baseUrl;
    private final boolean verboseErrors;
    private final Map<Class<? extends Settings>, Settings> settings;

    public ElideSettings(AuditLogger auditLogger, DataStore dataStore, EntityDictionary entityDictionary,
            ObjectMapper objectMapper, Function<RequestScope, PermissionExecutor> permissionExecutor,
            HeaderProcessor headerProcessor, int maxPageSize, int defaultPageSize, Serdes serdes, String baseUrl,
            boolean verboseErrors, Map<Class<? extends Settings>, Settings> settings) {
        super();
        this.auditLogger = auditLogger;
        this.dataStore = dataStore;
        this.entityDictionary = entityDictionary;
        this.objectMapper = objectMapper;
        this.permissionExecutor = permissionExecutor;
        this.headerProcessor = headerProcessor;
        this.maxPageSize = maxPageSize;
        this.defaultPageSize = defaultPageSize;
        this.serdes = serdes;
        this.baseUrl = baseUrl;
        this.verboseErrors = verboseErrors;
        this.settings = settings;
    }

    /**
     * Returns a builder with the current values.
     *
     * @return the builder to mutate
     */
    public ElideSettingsBuilder mutate() {
        ElideSettingsBuilder builder = ElideSettings.builder()
                .auditLogger(this.auditLogger)
                .dataStore(this.dataStore)
                .entityDictionary(this.entityDictionary)
                .objectMapper(this.objectMapper)
                .permissionExecutor(this.permissionExecutor)
                .headerProcessor(this.headerProcessor)
                .maxPageSize(this.maxPageSize)
                .defaultPageSize(this.defaultPageSize)
                .baseUrl(this.baseUrl)
                .verboseErrors(this.verboseErrors);

        builder.serdes(newSerdes -> newSerdes.entries(entries -> {
            entries.clear(); // Clear the defaults when copying
            this.serdes.entrySet().stream().forEach(entry -> entries.put(entry.getKey(), entry.getValue()));
        }));

        this.settings.values().forEach(oldSettings -> builder.settings(oldSettings.mutate()));
        return builder;
    }

    /**
     * Gets the specific {@link Settings} or null if not present.
     *
     * @param <T> the settings type
     * @param clazz the settings class
     * @return the settings
     */
    public <T extends Settings> T getSettings(Class<T> clazz) {
        return clazz.cast(this.settings.get(clazz));
    }

    /**
     * Returns a mutable {@link ElideSettingsBuilder} for building {@link ElideSettings}.
     *
     * @return the builder
     */
    public static ElideSettingsBuilder builder() {
        return new ElideSettingsBuilder();
    }

    /**
     * A mutable builder for building {@link ElideSettings}.
     */
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
                    this.permissionExecutor, this.headerProcessor, this.maxPageSize,
                    this.defaultPageSize, this.serdes.build(), this.baseUrl, this.verboseErrors, settings);
        }
    }

    public abstract static class ElideSettingsBuilderSupport<S> {
        protected Serdes.SerdesBuilder serdes = Serdes.builder();
        protected String baseUrl = "";
        protected AuditLogger auditLogger = new Slf4jLogger();
        protected HeaderProcessor headerProcessor = Headers::removeAuthorizationHeaders;
        protected ObjectMapper objectMapper = new ObjectMapper();
        protected int maxPageSize = Pagination.MAX_PAGE_SIZE;
        protected int defaultPageSize = Pagination.DEFAULT_PAGE_SIZE;
        protected Function<RequestScope, PermissionExecutor> permissionExecutor = ActivePermissionExecutor::new;
        protected Map<Class<?>, Settings.SettingsBuilder> settings = new LinkedHashMap<>();
        protected DataStore dataStore;
        protected EntityDictionary entityDictionary;
        protected ExceptionMappers exceptionMappers;
        protected boolean verboseErrors = false;

        protected ElideSettingsBuilderSupport() {
            // By default, Elide supports epoch based dates.
            // This can be cleared by serdes -> serdes.clear()
            this.serdes.withDefaults().withEpochDates().build();
        }

        public <T extends Settings.SettingsBuilder> T getSettings(Class<T> clazz) {
            return clazz.cast(this.settings.get(clazz));
        }

        /**
         * Customize the serializers and deserializers in the {@link SerdesBuilder}.
         *
         * @param serdes the customizer
         * @return the builder
         */
        public S serdes(Consumer<SerdesBuilder> serdes) {
            serdes.accept(this.serdes);
            return self();
        }

        /**
         * Add the {@link SettingsBuilder}.
         *
         * @param settings the settings builders
         * @return the builder
         */
        public S settings(SettingsBuilder... settings) {
            Arrays.asList(settings).stream().forEach(item -> this.settings.put(item.getClass(), item));
            return self();
        }

        /**
         * Customize the {@link SettingsBuilder}.
         *
         * @param settings the customizer
         * @return the builder
         */
        public S settings(Consumer<Map<Class<?>, SettingsBuilder>> settings) {
            settings.accept(this.settings);
            return self();
        }

        /**
         * Enable or disable verbose error message generation.
         *
         * @param verboseErrors set true to enable
         * @return the builder
         */
        public S verboseErrors(boolean verboseErrors) {
            this.verboseErrors = verboseErrors;
            return self();
        }

        /**
         * Sets the base url.
         *
         * @param baseUrl the base url
         * @return the builder
         */
        public S baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return self();
        }

        /**
         * Sets the {@link AuditLogger}.
         *
         * @param auditLogger the audit logger
         * @return the builder
         */
        public S auditLogger(AuditLogger auditLogger) {
            this.auditLogger = auditLogger;
            return self();
        }

        /**
         * Sets the {@link HeaderProcessor} to pre-process request headers.
         *
         * @param headerProcessor the header processor
         * @return the builder
         */
        public S headerProcessor(HeaderProcessor headerProcessor) {
            this.headerProcessor = headerProcessor;
            return self();
        }

        /**
         * Sets the Jackson {@link ObjectMapper} for serialization and deserialization.
         *
         * @param objectMapper the object mapper
         * @return the builder
         */
        public S objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return self();
        }

        /**
         * Sets the max page size.
         *
         * @param maxPageSize the max page size
         * @return the builder
         */
        public S maxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
            return self();
        }

        /**
         * Sets the default page size.
         *
         * @param defaultPageSize the default page size
         * @return the builder
         */
        public S defaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
            return self();
        }

        /**
         * Sets the {@link PermissionExecutor} resolver for a request scope.
         *
         * @param permissionExecutor the permission executor
         * @return the builder
         */
        public S permissionExecutor(Function<RequestScope, PermissionExecutor> permissionExecutor) {
            this.permissionExecutor = permissionExecutor;
            return self();
        }

        /**
         * Sets the {@link DataStore}.
         *
         * @param dataStore the data store
         * @return the builder
         */
        public S dataStore(DataStore dataStore) {
            this.dataStore = dataStore;
            return self();
        }

        /**
         * Sets the {@link EntityManager}.
         *
         * @param entityDictionary the entity dictionary
         * @return the builder
         */
        public S entityDictionary(EntityDictionary entityDictionary) {
            this.entityDictionary = entityDictionary;
            return self();
        }

        public abstract S self();
    }
}
