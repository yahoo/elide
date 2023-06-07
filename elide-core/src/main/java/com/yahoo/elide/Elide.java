/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.exceptions.CustomErrorException;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Set;


/**
 * REST Entry point handler.
 */
@Slf4j
public class Elide {
    @Getter private final ElideSettings elideSettings;
    @Getter private final AuditLogger auditLogger;
    @Getter private final DataStore dataStore;
    @Getter private final JsonApiMapper mapper;
    @Getter private final ErrorMapper errorMapper;
    @Getter private final TransactionRegistry transactionRegistry;
    @Getter private final ClassScanner scanner;
    private boolean initialized = false;

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object.
     */
    public Elide(
            ElideSettings elideSettings
    ) {
        this(elideSettings, new TransactionRegistry(), elideSettings.getDictionary().getScanner(), false);
    }

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object.
     * @param transactionRegistry Global transaction state.
     */
    public Elide(
            ElideSettings elideSettings,
            TransactionRegistry transactionRegistry
    ) {
        this(elideSettings, transactionRegistry, elideSettings.getDictionary().getScanner(), false);
    }

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object.
     * @param transactionRegistry Global transaction state.
     * @param scanner Scans classes for Elide annotations.
     * @param doScans Perform scans now.
     */
    public Elide(
            ElideSettings elideSettings,
            TransactionRegistry transactionRegistry,
            ClassScanner scanner,
            boolean doScans
    ) {
        this.elideSettings = elideSettings;
        this.scanner = scanner;
        this.auditLogger = elideSettings.getAuditLogger();
        this.dataStore = new InMemoryDataStore(elideSettings.getDataStore());
        this.mapper = elideSettings.getMapper();
        this.errorMapper = elideSettings.getErrorMapper();
        this.transactionRegistry = transactionRegistry;

        if (doScans) {
            doScans();
        }
    }

    /**
     * Scans & binds Elide models, scans for security check definitions, serde definitions, life cycle hooks
     * and more.  Any dependency injection required by objects found from scans must be performed prior to this call.
     */
    public void doScans() {
        if (! initialized) {
            elideSettings.getSerdes().forEach((type, serde) -> registerCustomSerde(type, serde, type.getSimpleName()));
            registerCustomSerde();

            //Scan for security checks prior to populating data stores in case they need them.
            elideSettings.getDictionary().scanForSecurityChecks();

            this.dataStore.populateEntityDictionary(elideSettings.getDictionary());
            initialized = true;
        }
    }

    protected void registerCustomSerde() {
        Injector injector = elideSettings.getDictionary().getInjector();
        Set<Class<?>> classes = registerCustomSerdeScan();

        for (Class<?> clazz : classes) {
            if (!Serde.class.isAssignableFrom(clazz)) {
                log.warn("Skipping Serde registration (not a Serde!): {}", clazz);
                continue;
            }
            Serde serde = (Serde) injector.instantiate(clazz);
            injector.inject(serde);

            ElideTypeConverter converter = clazz.getAnnotation(ElideTypeConverter.class);
            Class baseType = converter.type();
            registerCustomSerde(baseType, serde, converter.name());

            for (Class type : converter.subTypes()) {
                if (!baseType.isAssignableFrom(type)) {
                    throw new IllegalArgumentException("Mentioned type " + type
                            + " not subtype of " + baseType);
                }
                registerCustomSerde(type, serde, converter.name());
            }
        }
    }

    protected void registerCustomSerde(Class<?> type, Serde serde, String name) {
        log.info("Registering serde for type : {}", type);
        CoerceUtil.register(type, serde);
        registerCustomSerdeInObjectMapper(type, serde, name);
    }

    protected void registerCustomSerdeInObjectMapper(Class<?> type, Serde serde, String name) {
        ObjectMapper objectMapper = mapper.getObjectMapper();
        objectMapper.registerModule(new SimpleModule(name)
                .addSerializer(type, new JsonSerializer<Object>() {
                    @Override
                    public void serialize(Object obj, JsonGenerator jsonGenerator,
                                          SerializerProvider serializerProvider)
                            throws IOException, JsonProcessingException {
                        jsonGenerator.writeObject(serde.serialize(obj));
                    }
                }));
    }

    protected Set<Class<?>> registerCustomSerdeScan() {
        return scanner.getAnnotatedClasses(ElideTypeConverter.class);
    }

    public CustomErrorException mapError(Exception error) {
        if (errorMapper != null) {
            log.trace("Attempting to map unknown exception of type {}", error.getClass());
            CustomErrorException customizedError = errorMapper.map(error);

            if (customizedError != null) {
                log.debug("Successfully mapped exception from type {} to {}",
                        error.getClass(), customizedError.getClass());
                return customizedError;
            } else {
                log.debug("No error mapping present for {}", error.getClass());
            }
        }

        return null;
    }
}
