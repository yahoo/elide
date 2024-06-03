# Elide Spring Autoconfigure

The Elide spring autoconfigure package provides the core code needed to use Elide with Spring Boot 3. It includes:

1. Rest API controllers for JSON-API, GraphQL, and OpenAPI documentation.
2. Configuration properties for common Elide settings.
3. Bean configurations for complete customization:

   1. `Elide` - provides complete control to configure Elide.
   2. `Entity Dictionary` - to register security checks and lifecycle hooks.
   3. `Data Store` - to override the default data store.
   4. `API Docs`- to control the OpenAPI document generation.

## Core Properties

| Name                                | Description                                                                                            | Default Value |
|-------------------------------------|--------------------------------------------------------------------------------------------------------|---------------|
| `elide.base-url`                    | The base service URL that clients use in queries.                                                      |               |
| `elide.default-page-size`           | Default pagination size for collections if the client doesn't set the pagination size.                 | `500`         |
| `elide.max-page-size`               | The maximum pagination size a client can request.                                                      | `10000`       |
| `elide.verbose-errors`              | Turns on/off verbose error responses.                                                                  | `false`       |
| `elide.strip-authorization-headers` | Remove Authorization headers from RequestScope to prevent accidental logging of security credentials.  | `true`        |

## API Versioning Strategy Properties

| Name                                                              | Description                                                                                    | Default Value    |
|-------------------------------------------------------------------|------------------------------------------------------------------------------------------------|------------------|
| `elide.api-versioning-strategy.path.enabled`                      | Whether or not the path based strategy is enabled.                                             | `true`           |
| `elide.api-versioning-strategy.path.version-prefix`               | The version prefix to use. For instance `/v1/resource`.                                        | `v`              |
| `elide.api-versioning-strategy.header.enabled`                    | Whether or not the header based strategy is enabled.                                           | `false`          |
| `elide.api-versioning-strategy.header.header-name`                | The header names that contains the API version. For instance `Accept-Version` or `ApiVersion`. | `Accept-Version` |
| `elide.api-versioning-strategy.parameter.enabled`                 | Whether or not the parameter based strategy is enabled.                                        | `false`          |
| `elide.api-versioning-strategy.parameter.parameter-name`          | The parameter name that contains the API version.                                              | `v`              |
| `elide.api-versioning-strategy.media-type-profile.enabled`        | Whether or not the media type profile based strategy is enabled.                               | `false`          |
| `elide.api-versioning-strategy.media-type-profile.version-prefix` | The version prefix to use for the version.                                                     | `v`              |
| `elide.api-versioning-strategy.media-type-profile.uri-prefix`     | The uri prefix to use to determine the profile that contains the API version.                  |                  |
|
## JSON-API Properties

| Name                           | Description                               | Default Value |
|--------------------------------|-------------------------------------------|---------------|
| `elide.json-api.enabled`       | Whether or not the controller is enabled. | `false`       |
| `elide.json-api.path`          | The URL path prefix for the controller.   | `/`           |
| `elide.json-api.links.enabled` | Turns on/off JSON-API links in the API.   | `false`       |

## GraphQL Properties

| Name                                                | Description                                                                                | Default Value |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------|---------------|
| `elide.graphql.enabled`                             | Whether or not the controller is enabled.                                                  | `false`       |
| `elide.graphql.path`                                | The URL path prefix for the controller.                                                    | `/`           |
| `elide.graphql.federation.enabled`                  | Turns on/off Apollo federation schema.                                                     | `false`       |
| `elide.graphql.subscription.enabled`                | Whether or not the controller is enabled.                                                  | `false`       |
| `elide.graphql.subscription.path`                   | The URL path prefix for the controller.                                                    | `/`           |
| `elide.graphql.subscription.send-ping-on-subscribe` | Websocket sends a PING immediate after receiving a SUBSCRIBE.                              | `false`       |
| `elide.graphql.subscription.connection-timeout`     | Time allowed from web socket creation to successfully receiving a CONNECTION_INIT message. | `5000ms`      |
| `elide.graphql.subscription.idle-timeout`           | Maximum idle timeout in milliseconds with no websocket activity.                           | `300000ms`    |
| `elide.graphql.subscription.max-subscriptions`      | Maximum number of outstanding GraphQL queries per websocket.                               | `30`          |
| `elide.graphql.subscription.max-message-size`       | Maximum message size that can be sent to the websocket.                                    | `10000`       |
| `elide.graphql.subscription.publishing.enabled`     | Whether Elide should publish subscription notifications to JMS on lifecycle events.        | `false`       |

## API Docs Properties

| Name                     | Description                                    | Default Value |
|--------------------------|------------------------------------------------|---------------|
| `elide.api-docs.enabled` | Whether or not the controller is enabled.      | `false`       |
| `elide.api-docs.path`    | The URL path prefix for the controller.        | `/`           |
| `elide.api-docs.version` | The OpenAPI Specification Version to generate. | `openapi-3-0` |

## Async Properties

| Name                                                    | Description                                                                                                             | Default Value |
|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|---------------|
| `elide.async.enabled`                                   | Whether or not the async feature is enabled.                                                                            | `false`       |
| `elide.async.thread-pool-size`                          | Default thread pool size.                                                                                               | `5`           |
| `elide.async.max-async-after`                           | Default maximum permissible time to wait synchronously for the query to complete before switching to asynchronous mode. | `10s`         |
| `elide.async.cleanup.enabled`                           | Whether or not the cleanup is enabled.                                                                                  | `false`       |
| `elide.async.cleanup.query-max-run-time`                | Maximum query run time.                                                                                                 | `3600s`       |
| `elide.async.cleanup.query-retention-duration`          | Retention period of async query and results before being cleaned up.                                                    | `7d`          |
| `elide.async.cleanup.query-cancellation-check-interval` | Polling interval to identify async queries that should be canceled.                                                     | `300s`        |
| `elide.async.export.enabled`                            | Whether or not the controller is enabled.                                                                               | `false`       |
| `elide.async.export.path`                               | The URL path prefix for the controller.                                                                                 | `/export`     |
| `elide.async.export.append-file-extension`              | Enable Adding Extension to table export attachments.                                                                    | `false`       |
| `elide.async.export.storage-destination`                | Storage engine destination.                                                                                             | `/tmp`        |
| `elide.async.export.format.csv.write-header`            | Generates the header in a CSV formatted export.                                                                         | `true`        |

## Aggregation Store Properties

| Name                                                        | Description                                                                           | Default Value |
|-------------------------------------------------------------|---------------------------------------------------------------------------------------|---------------|
| `elide.aggregation-store.enabled`                           | Whether or not aggregation data store is enabled.                                     | `false`       |
| `elide.aggregation-store.default-dialect`                   | SQLDialect type for default DataSource Object.                                        | `Hive`        |
| `elide.aggregation-store.query-cache.enabled`               | Whether or not to enable the query cache.                                             | `true`        |
| `elide.aggregation-store.query-cache.expiration`            | Query cache expiration after write.                                                   | `10m`         |
| `elide.aggregation-store.query-cache.max-size`              | Limit on number of query cache entries.                                               | `1024`        |
| `elide.aggregation-store.metadata-store.enabled`            | Whether or not meta data store is enabled.                                            | `false`       |
| `elide.aggregation-store.dynamic-config.enabled`            | Whether or not dynamic model config is enabled.                                       | `false`       |
| `elide.aggregation-store.dynamic-config.path`               | The path where the config hjsons are stored.                                          | `/`           |
| `elide.aggregation-store.dynamic-config.config-api.enabled` | Enable support for reading and manipulating HJSON configuration through Elide models. | `false`       |

## JPA Store Properties

| Name                                          | Description                                                                                                                            | Default Value |
|-----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `elide.jpa-store.delegate-to-in-memory-store` | When fetching a subcollection from another multi-element collection, whether or not to do sorting, filtering and pagination in memory. | `true`        |

## Entity Dictionary Override

By default, auto configuration creates an `EntityDictionary` with no checks or life cycle hooks registered. It does register spring as the dependency injection framework for Elide model injection.

```java
@Configuration
public class ElideConfiguration {
    @Bean
    public EntityDictionary entityDictionary(AutowireCapableBeanFactory beanFactory) {
        return new EntityDictionary(new HashMap<>(), beanFactory::autowireBean);
    }
}
```

A typical override would add some checks and life cycle hooks.  *This is likely the only override you'll need*:

```java
@Configuration
public class ElideConfiguration {
    @Bean
    public EntityDictionary entityDictionary(AutowireCapableBeanFactory beanFactory) {
        HashMap<String, Class<? extends Check>> checkMappings = new HashMap<>();
        checkMappings.put("allow all", Role.ALL.class);
        checkMappings.put("deny all", Role.NONE.class);

        EntityDictionary dictionary = new EntityDictionary(checkMappings, beanFactory::autowireBean);
        dictionary.bindTrigger(Book.class, OnCreatePostCommit.class, (book, scope, changes) -> { /* DO SOMETHING */ });
        dictionary.bindTrigger(Book.class, OnUpdatePostCommit.class, "title", (book, scope, changes) -> { /* DO SOMETHING */ });
    }
}
```

## Data Store Override
By default, the auto configuration will wire up a JPA data store:

```java
@Configuration
public class ElideConfiguration {
    @Bean
    public DataStore dataStore(EntityManagerFactory entityManagerFactory) {
        return new JpaDataStore(
                () -> { return entityManagerFactory.createEntityManager(); },
                (em -> { return new NonJtaTransaction(em); }));
    }
}
```

Override this bean if you want a different store or multiple stores.

## OpenAPI Override

By default, Elide will generate the OpenAPI documentation for every model exposed into a single OpenAPI document. One document will be generated for each API version.

```java
@Configuration
public class ElideConfiguration {
    @Bean
    public ApiDocsController.ApiDocsRegistrations apiDocsRegistrations(RefreshableElide elide,
        ElideConfigProperties settings, OpenApiDocumentCustomizer customizer) {
        return buildApiDocsRegistrations(elide, settings, customizer);
    }
}
```

You'll want to override this if:

1. You don't want to expose all your models via OpenAPI.
2. You want to break up your models into multiple OpenAPI documents.

The API Docs controller will also accept a `ApiDocsRegistrations` bean. This will break the OpenAPI document into multiple documents. They key of the map is the URL prefix for each separate document exposed.

If you just wish to perform customization of the OpenAPI document that is generated by default, you can create a `OpenApiDocumentCustomizer` bean. Note that this will replace the automatically registered `BasicOpenApiDocumentCustomizer` which loads additional details such as the title of the API from a `OpenAPIDefinition` annotated bean. If this is still required you can extend the `BasicOpenApiDocumentCustomizer`.

```java
@Configuration
public class ElideConfiguration {
    @Bean
    public OpenApiDocumentCustomizer openApiDocumentCustomizer() {
        return new CustomOpenApiDocumentCustomizer();
    }
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
@Configuration
public class ElideConfiguration {
    @Bean
    Elide elide(EntityDictionary entityDictionary, DataStore dataStore, ElideConfigProperties settings) {
        ElideSettingsBuilder builder = ElideSettings.builder()
                .dataStore(dataStore)
                .entityDictionary(entityDictionary)
                .maxPageSize(settings.getMaxPageSize())
                .defaultPageSize(settings.getDefaultPageSize())
                .auditLogger(new Slf4jLogger())
                .settings(JsonApiSettings.builder()
                        .joinFilterDialect(RSQLFilterDialect.builder().dictionary(entityDictionary).build())
                        .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(entityDictionary).build()))
                .serdes(serdes -> serdes.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC")));
        return new Elide(builder.build());
    }
}
```
