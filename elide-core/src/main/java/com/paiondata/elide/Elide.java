/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import com.paiondata.elide.core.SerdeRegistrations;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.audit.AuditLogger;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.inmemory.InMemoryDataStore;
import com.paiondata.elide.core.dictionary.Injector;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;
import com.paiondata.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.paiondata.elide.core.utils.coerce.converters.Serde;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;


/**
 * Elide.
 */
@Slf4j
public class Elide {
    @Getter private final ElideSettings elideSettings;
    @Getter private final AuditLogger auditLogger;
    @Getter private final DataStore dataStore;
    @Getter private final ObjectMapper objectMapper;
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
        this(elideSettings, new TransactionRegistry(), elideSettings.getEntityDictionary().getScanner(), false);
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
        this(elideSettings, transactionRegistry, elideSettings.getEntityDictionary().getScanner(), false);
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
        this.objectMapper = elideSettings.getObjectMapper();
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
            elideSettings.getEntityDictionary().scanForSecurityChecks();

            this.dataStore.populateEntityDictionary(elideSettings.getEntityDictionary());
            initialized = true;
        }
    }

    protected void registerCustomSerde() {
        Injector injector = elideSettings.getEntityDictionary().getInjector();
        Set<Class<?>> classes = registerCustomSerdeScan();

        for (Class<?> clazz : classes) {
            if (!Serde.class.isAssignableFrom(clazz)) {
                log.warn("Skipping Serde registration (not a Serde!): {}", clazz);
                continue;
            }
            Serde serde = (Serde) injector.instantiate(clazz);
            injector.inject(serde);

            ElideTypeConverter converter = clazz.getAnnotation(ElideTypeConverter.class);
            Class<?> baseType = converter.type();
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

    protected <S, T> void registerCustomSerde(Class<T> type, Serde<S, T> serde, String name) {
        log.info("Registering serde for type : {}", type);
        CoerceUtil.register(type, serde);
        registerCustomSerdeInObjectMapper(type, serde, name);
    }

    protected <S, T> void registerCustomSerdeInObjectMapper(Class<T> type, Serde<S, T> serde, String name) {
        SerdeRegistrations.register(objectMapper, type, serde, name);
    }

    protected Set<Class<?>> registerCustomSerdeScan() {
        return scanner.getAnnotatedClasses(ElideTypeConverter.class);
    }

    public <T extends Settings> T getSettings(Class<T> clazz) {
        return this.elideSettings.getSettings(clazz);
    }
}
