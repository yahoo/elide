# Elide Spring Autoconfigure

The Elide spring autoconfigure package provides the core code needed to use Elide with Spring Boot 2. It includes:
1. Rest API controllers for JSON-API, GraphQL, and Swagger documentation.
2. Configuration properties for common Elide settings.
3. Bean configurations for complete customization:
   1. `Elide` - provides complete control to configure Elide.
   2. `Entity Dictionary` - to register security checks and lifecycle hooks.
   3. `Data Store` - to override the default data store.
   4. `Swagger` - to control the Swagger document generation.
   
## Configuration Properties   

| *Property*                 | *Required* |  *Default*      | *Description*                                            |
| -------------------------- | -----------| --------------- | -------------------------------------------------------- |
| elide.pageSize             | No         | 500             | Default pagination page size for collections             |
| elide.maxPageSize          | No         | 10000           | Max pagination page size a client can request.           |
| elide.json-api.path        | No         | '/'             | URL path prefix for JSON-API endpoint.                   |
| elide.json-api.enabled     | No         | false           | Whether or not the JSON-API endpoint is exposed.         |
| elide.graphql.path         | No         | '/'             | URL path prefix for GraphQL endpoint.                    |
| elide.graphql.enabled      | No         | false           | Whether or not the GraphQL endpoint is exposed.          |
| elide.swagger.path         | No         | '/'             | URL path prefix for Swagger document  endpoint.          |
| elide.swagger.enabled      | No         | false           | Whether or not the Swagger document endpoint is exposed. |
| elide.swagger.name         | No         | 'Elide Service' | Swagger documentation requires an API name.              |
| elide.swagger.version      | No         | '1.0'           | Swagger documentation requires an API version.           |

## Entity Dictionary Override

By default, auto configuration creates an `EntityDictionary` with no checks or life cycle hooks registered. It does register spring as the dependency injection framework for Elide model injection.

```java
    @Bean
    public EntityDictionary buildDictionary(AutowireCapableBeanFactory beanFactory) {
        return new EntityDictionary(new HashMap<>(), beanFactory::autowireBean);
    }
```

A typical override would add some checks and life cycle hooks.  *This is likely the only override you'll need*:

```java
    @Bean
    public EntityDictionary buildDictionary(AutowireCapableBeanFactory beanFactory) {
        HashMap<String, Class<? extends Check>> checkMappings = new HashMap<>();
        checkMappings.put("allow all", Role.ALL.class);
        checkMappings.put("deny all", Role.NONE.class);

        EntityDictionary dictionary = new EntityDictionary(checkMappings, beanFactory::autowireBean);
        dictionary.bindTrigger(Book.class, OnCreatePostCommit.class, (book, scope, changes) -> { /* DO SOMETHING */ }); 
        dictionary.bindTrigger(Book.class, OnUpdatePostCommit.class, "title", (book, scope, changes) -> { /* DO SOMETHING */ });
    }
```

## Data Store Override
By default, the auto configuration will wire up a JPA data store:

```java
    @Bean
    public DataStore buildDataStore(EntityManagerFactory entityManagerFactory) {
        return new JpaDataStore(
                () -> { return entityManagerFactory.createEntityManager(); },
                (em -> { return new NonJtaTransaction(em); }));
    }
```

Override this bean if you want a different store or multiple stores.

## Swagger Override

By default, Elide will generate swagger documentation for every model exposed into a single swagger document:

```java
    @Bean
    public Swagger buildSwagger(EntityDictionary dictionary, ElideConfigProperties settings) {
        Info info = new Info()
                .title(settings.getSwagger().getName())
                .version(settings.getSwagger().getVersion());

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);

        Swagger swagger = builder.build().basePath(settings.getJsonApi().getPath());

        return swagger;
    }
```

You'll want to override this if:
1. You want to configure authentication for your swagger endpoint.
2. You don't want to expose all your models via swagger.
3. You want to break up your models into multiple swagger documents.

The Swagger controller will also except a bean that returns: `Map<String, Swagger`. This will break the swagger document into multiple documents.  They key of the map is the URL prefix for each separate document exposed.

## Elide Override

This provides complete control to manipulate the `ElideSettings` object that configures Elide.

Reasons for doing this might include:
1. Using epoch based dates instead of ISO8601 date strings.
2. Registering a custom `Serde` for type coercion.
3. Changing the Elide filter dialect.
4. Configuring a custom Elide audit logger.

```java
    @Bean
    public Elide initializeElide(EntityDictionary dictionary,
                          DataStore dataStore, ElideConfigProperties settings) {

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withDefaultMaxPageSize(settings.getMaxPageSize())
                .withDefaultPageSize(settings.getPageSize())
                .withUseFilterExpressions(true)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withAuditLogger(new Slf4jLogger())
                .withEncodeErrorResponses(true)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));

        return new Elide(builder.build());
```
