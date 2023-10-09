# Elide Spring Autoconfigure

The Elide spring autoconfigure package provides the core code needed to use Elide with Spring Boot 3. It includes:
1. Rest API controllers for JSON-API, GraphQL, and OpenAPI documentation.
2. Configuration properties for common Elide settings.
3. Bean configurations for complete customization:
   1. `Elide` - provides complete control to configure Elide.
   2. `Entity Dictionary` - to register security checks and lifecycle hooks.
   3. `Data Store` - to override the default data store.
   4. `API Docs`- to control the OpenAPI document generation.
   
## Configuration Properties   

| *Property*                        | *Required* |  *Default*      | *Description*                                                                     |
| --------------------------------- | -----------| --------------- | --------------------------------------------------------------------------------- |
| elide.page-size                   | No         | 500             | Default pagination page size for collections                                      |
| elide.max-page-size               | No         | 10000           | Max pagination page size a client can request.                                    |
| elide.json-api.path               | No         | '/'             | URL path prefix for JSON-API endpoint.                                            |
| elide.json-api.enabled            | No         | false           | Whether or not the JSON-API endpoint is exposed.                                  |
| elide.graphql.path                | No         | '/'             | URL path prefix for GraphQL endpoint.                                             |
| elide.graphql.enabled             | No         | false           | Whether or not the GraphQL endpoint is exposed.                                   |
| elide.api-docs.path               | No         | '/'             | URL path prefix for OpenAPI document endpoint.                                    |
| elide.api-docs.enabled            | No         | false           | Whether or not the OpenAPI document endpoint is exposed.                          |
| elide.api-docs.version            | No         | openapi_3_0     | The OpenAPI Specification Version to generate. Either openapi_3_0 or openapi_3_1. |
| elide.query-cache-maximum-entries | No         | 1024            | Maximum number of entries in query result cache.                                  |


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

## OpenAPI Override

By default, Elide will generate the OpenAPI documentation for every model exposed into a single OpenAPI document. One document will be generated for each API version.

```java
    @Bean
    public ApiDocsController.ApiDocsRegistrations apiDocsRegistrations(RefreshableElide elide,
        ElideConfigProperties settings, OpenApiDocumentCustomizer customizer) {
        return buildApiDocsRegistrations(elide, settings, customizer);
    }    
```

You'll want to override this if:
1. You don't want to expose all your models via OpenAPI.
2. You want to break up your models into multiple OpenAPI documents.

The API Docs controller will also accept a `ApiDocsRegistrations` bean. This will break the OpenAPI document into multiple documents. They key of the map is the URL prefix for each separate document exposed.

If you just wish to perform customization of the OpenAPI document that is generated by default, you can create a `OpenApiDocumentCustomizer` bean. Note that this will replace the automatically registered `BasicOpenApiDocumentCustomizer` which loads additional details such as the title of the API from a `OpenAPIDefinition` annotated bean. If this is still required you can extend the `BasicOpenApiDocumentCustomizer`.

```java
    @Bean
    public OpenApiDocumentCustomizer openApiDocumentCustomizer() {
        return new CustomOpenApiDocumentCustomizer();
    }    
```

The API Docs controller can generate both the JSON or YAML version of the OpenAPI document. To generate the YAML version the requested content type should be `application/yaml`.

### API Information

The API information such as the title can be set by the application by annotating a bean with the `OpenAPIDefinition` annotation.

```java
@SpringBootApplication
@EntityScan
@OpenAPIDefinition(info = @Info(title = "My Title", description = "My Description"))
public class App {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(App.class, args);
    }
}
```

### SpringDoc Integration

By default, Elide will create a `OpenApiCustomizer` that will add all the models into SpringDoc's OpenAPI document. You can override this by defining an instance of a `ElideOpenApiCustomizer` bean. Note that this will only add models corresponding to the default API version.

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

        ElideSettingsBuilder builder = ElideSettings.builder().dataStore(dataStore)
                .entityDictionary(dictionary)
                .defaultMaxPageSize(settings.getMaxPageSize())
                .defaultPageSize(settings.getPageSize())
                .withUseFilterExpressions(true)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .auditLogger(new Slf4jLogger())
                .withEncodeErrorResponses(true)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));

        return new Elide(builder.build());
```
