/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import org.aeonbits.owner.Config;

import static org.aeonbits.owner.Config.Sources;

/**
 * Runtime settings for standalone Elide application.
 */
@Sources({ "file:${elide.settings}",
           "file:./settings/elide-settings.properties",
           "classpath:./elide-settings.properties" })
public interface RuntimeSettings extends Config {
    /**
     * Port for HTTP server to listen on.
     *
     * Default: 8080
     */
    @DefaultValue("8080")
    int port();

    /**
     * Package name containing your models. This package will be recursively scanned for @Entity's and
     * registered with Elide.
     *
     * NOTE: This will scan for all entities in that package and bind this data to a set named "elideAllModels".
     *       If providing a custom ElideSettings object, you can inject this data into your class by using:
     *
     *       <strong>@Inject @Named("elideAllModels") Set<Class> entities;</strong>
     *
     * Default: com.yourcompany.elide.models
     */
    @DefaultValue("com.yourcompany.elide.models")
    String modelPackage();

    /**
     * Class containing Elide service settings and overrides for your application as defined by the methods below.
     *
     * <strong>Must contain the following method signatures:</strong>
     * <ul>
     *     <li>One of:
     *       <ul>
     *         <li><strong>public ElideSettings getElideSettings()</strong></li>
     *         <li><strong>public Map<String, Class<? extends Check>> getCheckMappings()</strong></li>
     *       </ul>
     *     </li>
     *     <li><strong>public DefaultOpaqueUserFunction getUserExtractionFunction()</strong></li>
     * </ul>
     *
     * The class is fully injectable.
     *
     * Default: com.yourcompany.elide.security.Settings
     */
    @DefaultValue("com.yourcompany.elide.security.Settings")
    String settingsClass();

    /**
     * API root path specification for JSON-API. Namely, this is the mount point of your API. By default it will look
     * something like:
     *   <strong>yourcompany.com/api/v1/YOUR_ENTITY</strong>
     *
     * Default: /api/v1/*
     */
    @DefaultValue("/api/v1/*")
    String jsonApiPathSpec();

    /**
     * JAX-RS filters to register with the web service. A comma separated list containing fully qualified class names.
     *
     * Default: ""
     */
    String filters();

    /**
     * Supplemental resource configuration for Elide application. This should be a fully qualified class name.
     * The class should have a <em>non-static</em> method with the following name:
     *
     * <strong>public void configure(ResourceConfig);</strong>
     *
     * This class is fully injectable.
     *
     * Default: null
     */
    String additionalApplicationConfiguration();

    /**
     * Location to hibernate5 config. This is only required if you're using the <em>default</em> ElideSettings object.
     * Namely, you are not providing your own through the settings class.
     *
     * Default: ./settings/hibernate.cfg.xml
     */
    @DefaultValue("./settings/hibernate.cfg.xml")
    String hibernate5Config();

    /**
     * Determine whether or not to run in demo mode. If demo mode is set to true, then the in-memory store will be used
     * instead of hibernate. Only used if no custom ElideSettings is provided.
     *
     * DefaultValue: false
     */
    @DefaultValue("false")
    boolean demoMode();
}
