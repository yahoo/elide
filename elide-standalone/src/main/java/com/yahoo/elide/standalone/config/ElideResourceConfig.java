/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import com.yahoo.elide.standalone.Util;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.Converter;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * Elide application resource configuration file.
 */
@Slf4j
public class ElideResourceConfig extends ResourceConfig {
    private final ElideStandaloneSettings settings;
    private final ServiceLocator injector;

    public static final String ELIDE_STANDALONE_SETTINGS_ATTR = "elideStandaloneSettings";

    /**
     * Constructor
     *
     * @param injector Injection instance for application
     */
    @Inject
    public ElideResourceConfig(ServiceLocator injector, @Context ServletContext servletContext) {
        this.injector = injector;

        settings = (ElideStandaloneSettings) servletContext.getAttribute(ELIDE_STANDALONE_SETTINGS_ATTR);

        // Bind things that should be injectable to the Settings class
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(Util.getAllEntities(settings.getModelPackageName())).to(Set.class).named("elideAllModels");
            }
        });

        // Bind to injector
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                ElideSettings elideSettings = settings.getElideSettings(injector);

                Elide elide = new Elide(elideSettings);
                ObjectMapper mapper = elide.getMapper().getObjectMapper();
                mapper.registerModule(
                        new SimpleModule("isoDate", Version.unknownVersion())
                                .addSerializer(Date.class, new JsonSerializer<Date>() {
                                    @Override
                                    public void serialize(Date date,
                                                          JsonGenerator jsonGenerator,
                                                          SerializerProvider serializerProvider)
                                            throws IOException, JsonProcessingException {
                                        if (date == null) {
                                            jsonGenerator.writeNull();
                                        } else {
                                            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                                            // You can set timezone however you wish
                                            df.setTimeZone(TimeZone.getDefault());
                                            jsonGenerator.writeObject(df.format(date));
                                        }
                                    }
                                })
                );

                CoerceUtil.register(new Converter() {

                    @Override
                    public <T> T convert(Class<T> aClass, Object o) {
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                        // You can set timezone however you wish
                        df.setTimeZone(TimeZone.getDefault());

                        if (String.class.isAssignableFrom(o.getClass())) {
                            try {
                                return (T) df.parse((String) o);
                            } catch (ParseException e) {
                                throw new IllegalArgumentException(e);
                            }
                        } else {
                            throw new IllegalArgumentException("Conversion must supply a String arugment: " + o);
                        }
                    }
                }, Date.class);

                // Bind elide instance for injection into endpoint
                bind(elide).to(Elide.class).named("elide");

                // Bind user extraction function for endpoint
                bind(settings.getUserExtractionFunction())
                        .to(DefaultOpaqueUserFunction.class).named("elideUserExtractionFunction");

                // Bind additional elements
                bind(elideSettings).to(ElideSettings.class);
                bind(elideSettings.getDictionary()).to(EntityDictionary.class);
                bind(elideSettings.getDataStore()).to(DataStore.class).named("elideDataStore");
            }
        });

        registerFilters(settings.getFilters());

        additionalConfiguration(settings.getApplicationConfigurator());
    }

    /**
     * Init the supplemental resource config
     */
    private void additionalConfiguration(Consumer<ResourceConfig> configurator) {
        // Inject into consumer if class is provided
        injector.inject(configurator);
        configurator.accept(this);
    }

    /**
     * Register provided JAX-RS filters.
     */
    private void registerFilters(List<Class<?>> filters) {
        filters.forEach(this::register);
    }
}
