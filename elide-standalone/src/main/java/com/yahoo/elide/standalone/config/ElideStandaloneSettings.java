/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.exceptions.ErrorMapper;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.DefaultQueryValidator;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.cache.CaffeineCache;
import com.yahoo.elide.datastores.aggregation.core.Slf4jQueryLogger;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.DataSourceConfiguration;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialectFactory;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.AggregateBeforeJoinOptimizer;
import com.yahoo.elide.datastores.aggregation.validator.TemplateConfigValidator;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.modelconfig.DBPasswordExtractor;
import com.yahoo.elide.modelconfig.DynamicConfiguration;
import com.yahoo.elide.modelconfig.store.ConfigDataStore;
import com.yahoo.elide.modelconfig.store.models.ConfigChecks;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import com.yahoo.elide.swagger.SwaggerBuilder;
import com.yahoo.elide.swagger.resources.DocEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;

import io.swagger.models.Info;
import io.swagger.models.Swagger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Interface for configuring an ElideStandalone application.
 */
public interface ElideStandaloneSettings {
    /* Elide settings */

     public final Consumer<EntityManager> TXCANCEL = em -> em.unwrap(Session.class).cancelQuery();

    /**
     * A map containing check mappings for security across Elide. If not provided, then an empty map is used.
     * In case of an empty map, checks can be referenced by their fully qualified class names.
     *
     * @return Check mappings.
     */
    default Map<String, Class<? extends Check>> getCheckMappings() {
        return Collections.emptyMap();
    }

    /**
     * A Set containing Types to be excluded from EntityDictionary's EntityBinding.
     * @return Set of Types.
     */
    default Set<Type<?>> getEntitiesToExclude() {
        Set<Type<?>> entitiesToExclude = new HashSet<>();
        ElideStandaloneAsyncSettings asyncProperties = getAsyncProperties();

        if (asyncProperties == null || !asyncProperties.enabled()) {
            entitiesToExclude.add(ClassType.of(AsyncQuery.class));
        }

        if (asyncProperties == null || !asyncProperties.enableExport()) {
            entitiesToExclude.add(ClassType.of(TableExport.class));
        }

        return entitiesToExclude;
    }

    /**
     * Elide settings to be used for bootstrapping the Elide service. By default, this method constructs an
     * ElideSettings object using the application overrides provided in this class. If this method is overridden,
     * the returned settings object is used over any additional Elide setting overrides.
     *
     * That is to say, if you intend to override this method, expect to fully configure the ElideSettings object to
     * your needs.
     *
     * @param dictionary EntityDictionary object.
     * @param dataStore Dastore object
     * @param mapper Object mapper
     * @return Configured ElideSettings object.
     */
    default ElideSettings getElideSettings(EntityDictionary dictionary, DataStore dataStore, JsonApiMapper mapper) {

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withErrorMapper(getErrorMapper())
                .withJoinFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .withSubqueryFilterDialect(RSQLFilterDialect.builder().dictionary(dictionary).build())
                .withBaseUrl(getBaseUrl())
                .withJsonApiPath(getJsonApiPathSpec().replaceAll("/\\*", ""))
                .withGraphQLApiPath(getGraphQLApiPathSpec().replaceAll("/\\*", ""))
                .withJsonApiMapper(mapper)
                .withAuditLogger(getAuditLogger());

        if (verboseErrors()) {
            builder.withVerboseErrors();
        }

        if (getAsyncProperties().enableExport()) {
            builder.withExportApiPath(getAsyncProperties().getExportApiPathSpec().replaceAll("/\\*", ""));
        }

        if (enableISO8601Dates()) {
            builder.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));
        }

        return builder.build();
    }

    /* Non-required application/server settings */
    /**
     * Port for HTTP server to listen on.
     *
     * @return Default: 8080
     */
    default int getPort() {
        return 8080;
    }

    /**
     * Package name containing your models. This package will be recursively scanned for @Entity's and
     * registered with Elide.
     *
     * NOTE: This will scan for all entities in that package and bind this data to a set named "elideAllModels".
     *       If providing a custom ElideSettings object, you can inject this data into your class by using:
     *
     *       <strong>@Inject @Named("elideAllModels") Set&lt;Class&gt; entities;</strong>
     *
     * @return Default: com.yourcompany.elide.models
     */
    default String getModelPackageName() {
        return "com.yourcompany.elide.models";
    }

    /**
     * API root path specification for JSON-API. Namely, this is the mount point of your API.
     * By default it will look something like:
     *   <strong>yourcompany.com/api/v1/YOUR_ENTITY</strong>
     *
     * @return Default: /api/v1/*
     */
    default String getJsonApiPathSpec() {
        return "/api/v1/*";
    }

    /**
     * API root path specification for the GraphQL endpoint. Namely, this is the root uri for GraphQL.
     *
     * @return Default: /graphql/api/v1
     */
    default String getGraphQLApiPathSpec() {
        return "/graphql/api/v1/*";
    }

    /**
     * API root path specification for the Swagger endpoint. Namely, this is the root uri for Swagger docs.
     *
     * @return Default: /swagger/*
     */
    default String getSwaggerPathSpec() {
        return "/swagger/*";
    }

    /**
     * Enable the JSONAPI endpoint. If false, the endpoint will be disabled.
     *
     * @return Default: True
     */
    default boolean enableJSONAPI() {
        return true;
    }

    /**
     * Enable the GraphQL endpoint. If false, the endpoint will be disabled.
     *
     * @return Default: True
     */
    default boolean enableGraphQL() {
        return true;
    }

    /**
     * Enable/disable verbose error responses.
     * @return Default: False
     */
    default boolean verboseErrors() {
        return false;
    }

    /**
     * Async Properties.
     *
     * @return AsyncProperties type object.
     */
    default ElideStandaloneAsyncSettings getAsyncProperties() {
        //Default Properties
        return new ElideStandaloneAsyncSettings() { };
    }

    /**
     * Analytic Properties.
     *
     * @return AnalyticProperties type object.
     */
    default ElideStandaloneAnalyticSettings getAnalyticProperties() {
        //Default Properties
        return new ElideStandaloneAnalyticSettings() { };
    }

    /**
     * Subscription Properties.
     *
     * @return SubscriptionProperties type object.
     */
    default ElideStandaloneSubscriptionSettings getSubscriptionProperties() {
        //Default Properties
        return new ElideStandaloneSubscriptionSettings() { };
    }

    /**
     * Whether Dates should be ISO8601 strings (true) or epochs (false).
     * @return whether ISO8601Dates are enabled.
     */
    default boolean enableISO8601Dates() {
        return true;
    }

    /**
     * Whether or not Codahale metrics, healthchecks, thread, ping, and admin servlet
     * should be enabled.
     * @return  whether ServiceMonitoring is enabled.
     */
    default boolean enableServiceMonitoring() {
        return true;
    }

    /**
     * Enable swagger documentation.
     * @return whether Swagger is enabled;
     */
    default boolean enableSwagger() {
        return false;
    }

    /**
     * Swagger documentation requires an API version.
     * The models with the same version are included.
     * @return swagger version;
     */
    default String getSwaggerVersion() {
        return NO_VERSION;
    }

    /**
     * The service base URL that clients use in queries.  Elide will reference this name
     * in any callback URLs returned by the service.  If not set, Elide uses the API request to generate the base URL.
     * @return The base URL of the service.
     */
    default String getBaseUrl() {
        return "";
    }

    /**
     * Swagger documentation requires an API name.
     * @return swagger service name;
     */
    default String getSwaggerName() {
        return "Elide Service";
    }

    /**
     * Creates a singular swagger document for JSON-API.
     * @param dictionary Contains the static metadata about Elide models. .
     * @return list of swagger registration objects.
     */
    default List<DocEndpoint.SwaggerRegistration> buildSwagger(EntityDictionary dictionary) {
        Info info = new Info()
                .title(getSwaggerName())
                .version(getSwaggerVersion());

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);

        String moduleBasePath = getJsonApiPathSpec().replaceAll("/\\*", "");

        Swagger swagger = builder.build().basePath(moduleBasePath);

        List<DocEndpoint.SwaggerRegistration> docs = new ArrayList<>();
        docs.add(new DocEndpoint.SwaggerRegistration("test", swagger));

        return docs;
    }

    /**
     * JAX-RS filters to register with the web service.
     *
     * @return Default: Empty
     */
    default List<Class<?>> getFilters() {
        return Collections.emptyList();
    }

    /**
     * Supplemental resource configuration for Elide application. This should be a fully qualified class name.
     *
     * Before calling the consumer, the class will be injected by the ServiceLocator.
     *
     * @return Default: null
     */
    default Consumer<ResourceConfig> getApplicationConfigurator() {
        // Do nothing by default
        return (x) -> { };
    }

    /**
     * Gets properties to configure the database.
     *
     * @return Default: ./settings/hibernate.cfg.xml
     */
    default Properties getDatabaseProperties() {
        return new Properties();
    }

    /**
     * A hook to directly modify the jetty servlet context handler as necessary.
     *
     * @param servletContextHandler ServletContextHandler in use by Elide standalone.
     */
    default void updateServletContextHandler(ServletContextHandler servletContextHandler) {
        // Do nothing
    }

    /**
     * Gets the audit logger for elide.
     *
     * @return Default: Slf4jLogger
     */
    default AuditLogger getAuditLogger() {
        return new Slf4jLogger();
    }

    /**
     * Get the query cache implementation. If null, query cache is disabled.
     *
     * @return Default: {@code new CaffeineCache(getQueryCacheSize())}
     */
    default Cache getQueryCache() {
        return getAnalyticProperties().getQueryCacheMaximumEntries() > 0
                ? new CaffeineCache(getAnalyticProperties().getQueryCacheMaximumEntries(),
                                    getAnalyticProperties().getDefaultCacheExpirationMinutes())
                : null;
    }

    /**
     * Gets the dynamic configuration for models, security roles, and database connection.
     * @param scanner Class scanner
     * @return Optional DynamicConfiguration
     * @throws IOException thrown when validator fails to read configuration.
     */
    default Optional<DynamicConfiguration> getDynamicConfiguration(ClassScanner scanner) throws IOException {
        DynamicConfigValidator validator = null;

        if (getAnalyticProperties().enableAggregationDataStore()
                        && getAnalyticProperties().enableDynamicModelConfig()) {
            validator = new DynamicConfigValidator(scanner, getAnalyticProperties().getDynamicConfigPath());
            validator.readAndValidateConfigs();
        }

        return Optional.ofNullable(validator);
    }

    /**
     * Provides the default Hikari DataSource Configuration.
     * @return An instance of DataSourceConfiguration.
     */
    default DataSourceConfiguration getDataSourceConfiguration() {
        return new DataSourceConfiguration() {
        };
    }

    /**
     * Gets the DataStore for elide.
     * @param metaDataStore MetaDataStore object.
     * @param aggregationDataStore AggregationDataStore object.
     * @param entityManagerFactory EntityManagerFactory object.
     * @return DataStore object initialized.
     */
    default DataStore getDataStore(MetaDataStore metaDataStore, AggregationDataStore aggregationDataStore,
            EntityManagerFactory entityManagerFactory) {

        List<DataStore> stores = new ArrayList<>();

        DataStore jpaDataStore = new JpaDataStore(
                () -> entityManagerFactory.createEntityManager(),
                em -> new NonJtaTransaction(em, TXCANCEL, DEFAULT_LOGGER, true, true));

        stores.add(jpaDataStore);

        if (getAnalyticProperties().enableDynamicModelConfigAPI()) {
            stores.add(new ConfigDataStore(getAnalyticProperties().getDynamicConfigPath(),
                    new TemplateConfigValidator(getClassScanner(), getAnalyticProperties().getDynamicConfigPath())));
        }

        stores.add(metaDataStore);
        stores.add(aggregationDataStore);

        return new MultiplexManager(stores.toArray(new DataStore[0]));
    }

    /**
     * Gets the DataStore for elide when aggregation store is disabled.
     * @param entityManagerFactory EntityManagerFactory object.
     * @return DataStore object initialized.
     */
    default DataStore getDataStore(EntityManagerFactory entityManagerFactory) {
        DataStore jpaDataStore = new JpaDataStore(
                () -> entityManagerFactory.createEntityManager(),
                em -> new NonJtaTransaction(em, TXCANCEL, DEFAULT_LOGGER, true, true));

        return jpaDataStore;
    }

    /**
     * Gets the AggregationDataStore for elide.
     * @param queryEngine query engine object.
     * @return AggregationDataStore object initialized.
     */
    default AggregationDataStore getAggregationDataStore(QueryEngine queryEngine) {
        AggregationDataStore.AggregationDataStoreBuilder aggregationDataStoreBuilder = AggregationDataStore.builder()
                .queryEngine(queryEngine).queryLogger(new Slf4jQueryLogger());

        if (getAnalyticProperties().enableDynamicModelConfig()) {
            aggregationDataStoreBuilder.dynamicCompiledClasses(queryEngine.getMetaDataStore().getDynamicTypes());
        }
        aggregationDataStoreBuilder.cache(getQueryCache());
        return aggregationDataStoreBuilder.build();
    }

    /**
     * Gets the EntityDictionary for elide.
     * @param injector Service locator for web service for dependency injection.
     * @param dynamicConfiguration optional dynamic config object.
     * @param entitiesToExclude set of Entities to exclude from binding.
     * @return EntityDictionary object initialized.
     */
    default EntityDictionary getEntityDictionary(ServiceLocator injector, ClassScanner scanner,
            Optional<DynamicConfiguration> dynamicConfiguration, Set<Type<?>> entitiesToExclude) {

        Map<String, Class<? extends Check>> checks = new HashMap<>();

        if (getAnalyticProperties().enableDynamicModelConfigAPI()) {
            checks.put(ConfigChecks.CAN_CREATE_CONFIG, ConfigChecks.CanNotCreate.class);
            checks.put(ConfigChecks.CAN_READ_CONFIG, ConfigChecks.CanNotRead.class);
            checks.put(ConfigChecks.CAN_DELETE_CONFIG, ConfigChecks.CanNotDelete.class);
            checks.put(ConfigChecks.CAN_UPDATE_CONFIG, ConfigChecks.CanNotUpdate.class);
        }

        EntityDictionary dictionary = new EntityDictionary(
                new HashMap<>(), //Checks
                new HashMap<>(), //Role Checks
                new Injector() {
                    @Override
                    public void inject(Object entity) {
                        injector.inject(entity);
                    }

                    @Override
                    public <T> T instantiate(Class<T> cls) {
                        return injector.create(cls);
                    }
                },
                CoerceUtil::lookup, //Serde Lookup
                entitiesToExclude,
                scanner);

        dynamicConfiguration.map(DynamicConfiguration::getRoles).orElseGet(Collections::emptySet).forEach(role ->
            dictionary.addRoleCheck(role, new Role.RoleMemberCheck(role))
        );

        return dictionary;
    }

    /**
     * Gets the metadatastore for elide.
     * @param scanner Class scanner.
     * @param dynamicConfiguration optional dynamic config object.
     * @return MetaDataStore object initialized.
     */
    default MetaDataStore getMetaDataStore(ClassScanner scanner,
            Optional<DynamicConfiguration> dynamicConfiguration) {
        boolean enableMetaDataStore = getAnalyticProperties().enableMetaDataStore();

        return dynamicConfiguration
                .map(dc -> new MetaDataStore(scanner, dc.getTables(),
                        dc.getNamespaceConfigurations(), enableMetaDataStore))
                .orElseGet(() -> new MetaDataStore(scanner, enableMetaDataStore));
    }

    /**
     * Gets the QueryEngine for elide.
     * @param metaDataStore MetaDataStore object.
     * @param defaultConnectionDetails default DataSource Object and SQLDialect Object.
     * @param dynamicConfiguration Optional dynamic config.
     * @param dataSourceConfiguration DataSource Configuration.
     * @param dbPasswordExtractor Password Extractor Implementation.
     * @return QueryEngine object initialized.
     */
    default QueryEngine getQueryEngine(MetaDataStore metaDataStore, ConnectionDetails defaultConnectionDetails,
                    Optional<DynamicConfiguration> dynamicConfiguration,
                    DataSourceConfiguration dataSourceConfiguration, DBPasswordExtractor dbPasswordExtractor) {
        if (dynamicConfiguration.isPresent()) {
            Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();

            dynamicConfiguration.get().getDatabaseConfigurations().forEach(dbConfig ->
                connectionDetailsMap.put(dbConfig.getName(),
                                new ConnectionDetails(
                                                dataSourceConfiguration.getDataSource(dbConfig, dbPasswordExtractor),
                                                SQLDialectFactory.getDialect(dbConfig.getDialect())))
            );

            Function<String, ConnectionDetails> connectionDetailsLookup = (name) -> {
                if (StringUtils.isEmpty(name)) {
                    return defaultConnectionDetails;
                }
                return Optional.ofNullable(connectionDetailsMap.get(name))
                        .orElseThrow(() -> new IllegalStateException("ConnectionDetails undefined for connection: "
                                + name));
            };

            return new SQLQueryEngine(metaDataStore, connectionDetailsLookup,
                    new HashSet<>(Arrays.asList(new AggregateBeforeJoinOptimizer(metaDataStore))),
                    new DefaultQueryPlanMerger(metaDataStore),
                    new DefaultQueryValidator(metaDataStore.getMetadataDictionary()));
        }
        return new SQLQueryEngine(metaDataStore, (unused) -> defaultConnectionDetails);
    }

    /**
     * Get the class scanner for this Elide instance.
     * @return class scanner implementation.
     */
    default ClassScanner getClassScanner() {
        return new DefaultClassScanner();
    }

    /**
     * Get the error mapper for this Elide instance. By default no errors will be mapped.
     *
     * @return error mapper implementation
     */
    default ErrorMapper getErrorMapper() {
        return error -> null;
    }

    /**
     * Get the Jackson object mapper for Elide.
     *
     * @return object mapper.
     */
    default JsonApiMapper getObjectMapper() {
        return new JsonApiMapper();
    }
}
