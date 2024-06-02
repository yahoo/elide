---
sidebar_position: 2
title: Elide Standalone
---

Running Elide standalone is pretty straight forward by essentially implementing a **Binder** and a **ResourceConfig**:

In addition, a working example is [here](https://github.com/paion-data/elide-standalone-example)

:::danger

The `hibernate.hbm2ddl.auto` in this example is set to `create`, which means each run of the standalone will erase the
database and re-create tables. Please do change this value if used in production

:::

[In order for dependency injection to work properly in Elide standalone](https://github.com/QubitPi/jersey-webservice-template/pull/29/files),
we must use [Jersey binder](https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/ioc.html#d0e17933), 
not [HK binder](https://javaee.github.io/hk2/)

```java

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binder;

/**
 * A binder factory builds a custom binder for the Jersey application.
 * <p>
 * The factory makes the component object instance that will eventually be passed to
 * {@link org.glassfish.jersey.server.ResourceConfig#register(Object)}.
 */
@Immutable
@ThreadSafe
public class BinderFactory {

    /**
     * Builds a hk2 Binder instance.
     * <p>
     * This binder should bind all relevant resources for runtime dependency injection.
     *
     * @param injector  A standard HK2 service locator
     *
     * @return a binder instance that will be registered by putting as a parameter to
     * {@link org.glassfish.jersey.server.ResourceConfig#register(Object)}
     */
    @NotNull
    public Binder buildBinder(final ServiceLocator injector) {
        return new AbstractBinder() {

            private static final Consumer<EntityManager> TXCANCEL = em -> em.unwrap(Session.class).cancelQuery();

            private final ClassScanner classScanner = new DefaultClassScanner();

            @Override
            protected void configure() {
                final ElideSettings elideSettings = buildElideSettings();

                bind(buildElide(elideSettings)).to(Elide.class).named("elide");
                bind(elideSettings).to(ElideSettings.class);
                bind(elideSettings.getDictionary()).to(EntityDictionary.class);
                bind(elideSettings.getDataStore()).to(DataStore.class).named("elideDataStore");
            }

            /**
             * Initializes Elide middleware service.
             *
             * @param elideSettings  An object for configuring various aspect of the Elide middleware
             *
             * @return a new instance
             */
            @NotNull
            private Elide buildElide(@NotNull final ElideSettings elideSettings) {
                return new Elide(
                        elideSettings,
                        new TransactionRegistry(),
                        elideSettings.getDictionary().getScanner(),
                        false
                );
            }

            /**
             * Initializes Elide config object.
             *
             * @return a new instance
             */
            @NotNull
            private ElideSettings buildElideSettings() {
                return new ElideSettingsBuilder(buildDataStore(buildEntityManagerFactory()))
                        .withEntityDictionary(buildEntityDictionary(injector))
                        .build();
            }

            /**
             * Initializes the Elide {@link DataStore} service with the specified {@link EntityManagerFactory}.
             *
             * @param entityManagerFactory  An object used to initialize JPA
             *
             * @return a new instance
             */
            @NotNull
            private DataStore buildDataStore(@NotNull final EntityManagerFactory entityManagerFactory) {
                return new JpaDataStore(
                        entityManagerFactory::createEntityManager,
                        em -> new NonJtaTransaction(em, TXCANCEL),
                        entityManagerFactory::getMetamodel
                );
            }

            /**
             * Initializes the {@link EntityManagerFactory} service used by Elide JPA.
             *
             * @return a new instance
             */
            @NotNull
            private EntityManagerFactory buildEntityManagerFactory() {
                final String modelPackageName = "com.mycompany.mymodel";

                final ClassLoader classLoader = null;

                final PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl(
                        "my-webservice",
                        getAllEntities(classScanner, modelPackageName),
                        getDefaultDbConfigs(),
                        classLoader
                );

                return new EntityManagerFactoryBuilderImpl(
                        new PersistenceUnitInfoDescriptor(persistenceUnitInfo),
                        new HashMap<>(),
                        classLoader
                ).build();
            }

            /**
             * Get all the entities in a package.
             *
             * @param scanner  An object that picks up entities by Elide annotation
             * @param packageName  A fully qualified package name under which contains all entities
             *
             * @return all entities found in the provided package.
             */
            @NotNull
            public static List<String> getAllEntities(
                    @NotNull final ClassScanner scanner,
                    @NotNull final String packageName
            ) {
                return scanner.getAnnotatedClasses(packageName, Entity.class).stream()
                        .map(Class::getName)
                        .collect(Collectors.toList());
            }

            /**
             * Returns a collection of DB configurations, including connecting credentials.
             * <p>
             * In addition, the configurations consumes all configs defined in {@link JpaDatastoreConfig}
             *
             * @return a new instance
             */
            @NotNull
            @SuppressWarnings("MultipleStringLiterals")
            private static Properties getDefaultDbConfigs() {
                final Properties dbProperties = new Properties();

                dbProperties.put("hibernate.show_sql", "true");
                dbProperties.put("hibernate.hbm2ddl.auto", "create");
                dbProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
                dbProperties.put("hibernate.current_session_context_class", "thread");
                dbProperties.put("hibernate.jdbc.use_scrollable_resultset", "true");

                // Collection Proxy & JDBC Batching
                dbProperties.put("hibernate.jdbc.batch_size", "50");
                dbProperties.put("hibernate.jdbc.fetch_size", "50");
                dbProperties.put("hibernate.default_batch_fetch_size", "100");

                // Hikari Connection Pool Settings
                dbProperties.putIfAbsent("hibernate.connection.provider_class",
                        "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
                dbProperties.putIfAbsent("hibernate.hikari.connectionTimeout", "20000");
                dbProperties.putIfAbsent("hibernate.hikari.maximumPoolSize", "30");
                dbProperties.putIfAbsent("hibernate.hikari.idleTimeout", "30000");

                dbProperties.put("jakarta.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver");
                dbProperties.put("jakarta.persistence.jdbc.url", "jdbc:mysql://db/minerva?serverTimezone=UTC");
                dbProperties.put("jakarta.persistence.jdbc.user", "root");
                dbProperties.put("jakarta.persistence.jdbc.password", "root");

                return dbProperties;
            }

            /**
             * Initializes the Elide {@link EntityDictionary} service with a given dependency injector.
             *
             * @param injector  A standard HK2 service locator used by Elide
             *
             * @return a new instance
             */
            @NotNull
            private EntityDictionary buildEntityDictionary(@NotNull final ServiceLocator injector) {
                return new EntityDictionary(
                        new HashMap<>(),
                        new HashMap<>(),
                        new Injector() {
                            @Override
                            public void inject(final Object entity) {
                                injector.inject(entity);
                            }

                            @Override
                            public <T> T instantiate(final Class<T> cls) {
                                return injector.create(cls);
                            }
                        },
                        CoerceUtil::lookup,
                        new HashSet<>(),
                        classScanner
                );
            }
        };
    }
}
```

```java
/**
 * The resource configuration for the web applications.
 */
@Immutable
@ThreadSafe
@ApplicationPath("/v1/data/")
public class ResourceConfig extends org.glassfish.jersey.server.ResourceConfig {

    private static final String GRAPHQL_ENDPOINT_PACKAGE = "com.paiondata.elide.graphql";
    private static final String JAON_API_ENDPOINT_PACKAGE = "com.paiondata.elide.jsonapi.resources";

    /**
     * DI Constructor.
     *
     * @param injector  A standard HK2 service locator
     */
    @Inject
    public ResourceConfig(@NotNull final ServiceLocator injector) {
        this(injector, new BinderFactory());
    }

    /**
     * Constructor that allows for finer dependency injection control.
     *
     * @param injector  A standard HK2 service locator
     * @param binderFactory  An object that produces resource binder
     */
    private ResourceConfig(@NotNull final ServiceLocator injector, @NotNull final BinderFactory binderFactory) {
        packages(JAON_API_ENDPOINT_PACKAGE, GRAPHQL_ENDPOINT_PACKAGE);

        register(binderFactory.buildBinder(injector));

        // Bind api docs to given endpoint
        // This looks strange, but Jersey binds its Abstract binders first, and then later it binds 'external'
        // binders (like this HK2 version). This allows breaking dependency injection into two phases.
        // Everything bound in the first phase can be accessed in the second phase.
        register(new org.glassfish.hk2.utilities.binding.AbstractBinder() {
            @Override
            protected void configure() {
                injector.getService(Elide.class, "elide").doScans();
            }
        });
    }
}
```
