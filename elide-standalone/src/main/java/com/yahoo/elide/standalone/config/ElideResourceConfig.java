/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.standalone.datastore.InjectionAwareHibernateStore;
import lombok.extern.slf4j.Slf4j;
import org.aeonbits.owner.ConfigCache;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.CoderMalfunctionError;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Elide application resource configuration file.
 */
@Slf4j
public class ElideResourceConfig extends ResourceConfig {
    private static final RuntimeSettings SETTINGS =
            ConfigCache.getOrCreate(RuntimeSettings.class, System.getProperties());
    private final Object settingsObject;
    private final ServiceLocator injector;

    /**
     * Constructor
     */
    @Inject
    public ElideResourceConfig(ServiceLocator injector) {
        this.injector = injector;

        // Bind things that should be injectable to the Settings class
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(getAllEntities(SETTINGS.modelPackage())).to(Set.class).named("elideAllModels");
            }
        });

        settingsObject = instantiateSettingsObject(SETTINGS.settingsClass());

        // Bind to injector
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                ElideSettings elideSettings = getElideSettings();

                if (elideSettings == null) {
                    log.debug("Trying to load check mappings to use _default_ settings.");
                    elideSettings = getDefaultElideSettings();
                }

                // Bind elide instance for injection into endpoint
                bind(new Elide(elideSettings)).to(Elide.class).named("elide");

                // Bind user extraction function for endpoint
                bind(getUserExtractionFunction())
                        .to(DefaultOpaqueUserFunction.class).named("elideUserExtractionFunction");

                // Bind additional elements
                bind(elideSettings).to(ElideSettings.class);
                bind(elideSettings.getDictionary()).to(EntityDictionary.class);
                bind(elideSettings.getDataStore()).to(DataStore.class).named("elideDataStore");
            }
        });

        registerFilters();

        additionalConfiguration();
    }

    /**
     * Init the supplemental resource config
     */
    private void additionalConfiguration() {
        String className = SETTINGS.additionalApplicationConfiguration();

        if (className == null || className.trim().isEmpty()) {
            return;
        }

        try {
            Object configurator = injector.createAndInitialize(Class.forName(className));
            configurator.getClass().getMethod("configure", ResourceConfig.class).invoke(configurator, this);
        } catch (IllegalAccessException | ClassNotFoundException
                | NoSuchMethodException | InvocationTargetException e) {
            log.error("Could not instantiate class: {}", SETTINGS.additionalApplicationConfiguration(), e);
            // Be better.
            throw new CoderMalfunctionError(e);
        }
    }

    /**
     * Register provided JAX-RS filters.
     */
    private void registerFilters() {
        String filtersString = SETTINGS.filters();
        if (filtersString == null || filtersString.isEmpty()) {
            return;
        }
        for (String filterClass : filtersString.split(",")) {
            try {
                Class filter = Class.forName(filterClass.trim());
                register(filter);
            } catch (ClassNotFoundException e) {
                log.error("Could not register filter {}.", filterClass, e);
                // Be better.
                throw new CoderMalfunctionError(e);
            }
        }
    }

    /**
     * Get all the entities in a package.
     *
     * @param packageName Package name
     * @return All entities found in package.
     */
    private HashSet<Class> getAllEntities(String packageName) {
        return new HashSet<>(new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
                .setUrls(ClasspathHelper.forClassLoader(ClassLoader.getSystemClassLoader()))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName))))
                .getTypesAnnotatedWith(Entity.class));
    }

    /**
     * Retrieve a hibernate session factory.
     *
     * @return Hibernate session factory.
     */
    private SessionFactory getSessionFactory() {
        StandardServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
                .configure(new File(SETTINGS.hibernate5Config()))
                .build();
        MetadataSources sources = new MetadataSources(standardRegistry);

        getAllEntities(SETTINGS.modelPackage()).forEach(sources::addAnnotatedClass);

        Metadata metaData =  sources.getMetadataBuilder().build();
        return metaData.getSessionFactoryBuilder().build();
    }

    /**
     * Get check mappings class.
     *
     * @return Map of check mappings.
     */
    private Map<String, Class<? extends Check>> getCheckMappings() {
        Map<String, Class<? extends Check>> mappings = (Map) executeSettingsObjectMethod("getCheckMappings");
        if (mappings == null) {
            log.info("Found `null` for provided mappings. Falling back to default");
            return new HashMap<>();
        }
        log.info("Found valid check mappings. Setting to provided value.");
        return mappings;
    }

    /**
     * Retrieve the user extraction function.
     *
     * @return Provided user extraction function or default function.
     */
    private DefaultOpaqueUserFunction getUserExtractionFunction() {
        DefaultOpaqueUserFunction userFn = (DefaultOpaqueUserFunction) executeSettingsObjectMethod(
                "getUserExtractionFunction");
        if (userFn == null) {
            log.info("Found `null` from provided user extraction function. Falling back to default.");
            return SecurityContext::getUserPrincipal;
        }
        log.info("Found valid user extraction function. Setting to provided value.");
        return userFn;
    }

    /**
     * Get the ElideSettings object.
     *
     * @return Elide settings.
     */
    private ElideSettings getElideSettings() {
        ElideSettings settings = (ElideSettings) executeSettingsObjectMethod("getElideSettings");
        if (settings == null) {
            log.error("Could not find or execute {}#getElideSettings", SETTINGS.settingsClass());
        }
        return settings;
    }

    /**
     * Get our default elide settings if none were provided.
     *
     * By default, this includes Elide with:
     * <ul>
     *     <li>Either hibernate5 (non demo-mode) or in-memory store (demo mode)</li>
     *     <li>Filter expressions enabled</li>
     *     <li>All core filter dialects enabled</li>
     * </ul>
     *
     * @return Elide settings.
     */
    private ElideSettings getDefaultElideSettings() {
        DataStore dataStore;
        if (SETTINGS.demoMode()) {
            dataStore = new InMemoryDataStore(Package.getPackage(SETTINGS.modelPackage()));
        } else {
            dataStore = new InjectionAwareHibernateStore(injector, getSessionFactory());
        }
        EntityDictionary dictionary = new EntityDictionary(getCheckMappings());

        return new ElideSettingsBuilder(dataStore)
                .withUseFilterExpressions(true)
                .withEntityDictionary(dictionary)
                .withSubqueryFilterDialect(getDefaultFilterDialect(dictionary))
                .withJoinFilterDialect(getDefaultFilterDialect(dictionary))
                .build();
    }

    /**
     * Find a specific class and 0 argument static method, and then invoke it.
     *
     * @param function Static method name
     * @return Result from static method or null if errors encountered.
     */
    private Object executeSettingsObjectMethod(String function) {
        String className = SETTINGS.settingsClass();
        try {
            return settingsObject.getClass().getMethod(function).invoke(settingsObject);
        } catch (NoSuchMethodException e) {
            log.info("Found class '{}' but could not find static method '{}'", className, function);
        } catch (IllegalAccessException e) {
            log.info("Found class '{}' and method '{}', but could not execute (illegal access)", className, function);
        } catch (InvocationTargetException e) {
            log.info("Invocation target exception caught trying to invoke {}#{}", className, function, e);
        }
        return null;
    }

    /**
     * Get the settings object.
     *
     * @param className Name of settings class
     * @return Settings object.
     */
    private Object instantiateSettingsObject(String className) {
        try {
            Class clazz = Class.forName(className);
            return injector.createAndInitialize(clazz);
        } catch (ClassNotFoundException e) {
            log.error("Could not find settings class: {}", className);
            // Be better.
            throw new CoderMalfunctionError(e);
        }
    }

    /**
     * Get the default filter dialect.
     *
     * @param dictionary Entity dictionary for filter dialects.
     * @return All standard filter dialects.
     */
    private MultipleFilterDialect getDefaultFilterDialect(EntityDictionary dictionary) {
        return new MultipleFilterDialect(
                Arrays.asList(new RSQLFilterDialect(dictionary), new DefaultFilterDialect(dictionary)),
                Arrays.asList(new RSQLFilterDialect(dictionary), new DefaultFilterDialect(dictionary)));
    }
}
