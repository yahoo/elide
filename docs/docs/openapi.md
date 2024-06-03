---
sidebar_position: 10
title: OpenAPI
description: JSON API documentations
---

Overview
--------

Elide supports the generation of [OpenAPI](https://www.openapis.org/) documentation from Elide annotated beans.
Specifically, it generates a JSON or YAML document conforming to the OpenAPI specification that can be used by tools
like [Swagger UI](http://swagger.io/) (among others) to explore, understand, and compose queries against our Elide API.

Only JSON-API endpoints are documented. The GraphQL API schema can be explored directly with tools like
[Graphiql](https://github.com/graphql/graphiql).

Features Supported
------------------

- **JaxRS & Spring Endpoint** - Elide ships with a customizable JaxRS endpoints that can publish one or more OpenAPI
  documents in both JSON or YAML.
- **Path Discovery** - Given a set of entities to explore, Elide will generate the minimum, cycle-free, de-duplicated
  set of URL paths in the OpenAPI document.
- **Filter by Primitive Attributes** - All _GET_ requests on entity collections include filter parameters for each
  primitive attribute.
- **Prune Fields** - All _GET_ requests support JSON-API sparse fields query parameter.
- **Include Top Level Relationships** - All _GET_ requests support the ability to include direct relationships.
- **Sort by Attribute** - All _GET_ requests support sort query parameters.
- **Pagination** - All _GET_ requests support pagination query parameters.
- **Permission Exposition** - Elide permissions are exported as documentation for entity schemas.
- **Model & Attribute Properties** - The _required_, _readOnly_, _example_, _value_, _description_, _title_,
  _accessMode_, _requiredMode_ fields are extracted from `Schema` annotations.
- **API Version Support** - Elide can create separate documents for different API versions.

Getting Started
---------------

### Maven

If we are not using [Elide Spring Starter][elide-spring] or [Elide Standalone][elide-standalone] (which package
swagger as a dependency), we will need to pull in the following elide dependencies :

```xml
<dependency>
  <groupId>com.paiondata.elide</groupId>
  <artifactId>elide-swagger</artifactId>
</dependency>

<dependency>
  <groupId>com.paiondata.elide</groupId>
  <artifactId>elide-core</artifactId>
</dependency>
```

Pull in swagger core:

```xml
<dependency>
  <groupId>io.swagger</groupId>
  <artifactId>swagger-core-jakarta</artifactId>
</dependency>
```

#### Spring Boot Configuration

If we are using
[Elide Spring Autoconfigure](https://github.com/paion-data/elide/tree/master/elide-spring/elide-spring-boot-autoconfigure),
we can customize the `OpenAPI` document by using a `OpenApiDocumentCustomizer` bean:

```java
@Configuration
public class ElideConfiguration {

    @Bean
    public OpenApiDocumentCustomizer openApiDocumentCustomizer() {
        return openApi -> {
            Info info = new Info().title("My Title");
            openApi.setInfo(info);
        };
    }
}
```

The application YAML file has settings:

- to enable the OpenAPI document endpoint
- to set the URL path for the OpenAPI document endpoint
- to set the OpenAPI specification version to generate either 3.0 or 3.1

For example:

```yaml
elide:
  api-docs:
    enabled: true
    path: /doc
    version: openapi_3_0
```

#### Supporting OAuth

If we want Swagger UI to acquire & use a bearer token from an OAuth identity provider, we can configure the OpenAPI
document by using annotations:

```java
@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "My Title"), security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
    )
public class App {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(App.class, args);
    }
}
```

#### SpringDoc Integration

Elide contributes to [Springdoc](https://springdoc.org)'s OpenAPI document by exposing a Springdoc `OpenApiCustomizer`
bean.

If API Versioning is used, only the Path strategy is supported when integrating with Springdoc as the other strategies
are difficult to document with OpenAPI.

The default implementation is implemented in `DefaultElideOpenApiCustomizer`. To override the behavior a
`ElideOpenApiCustomizer` bean can be created which will cause the `DefaultElideOpenApiCustomizer` not to be configured.

```java
@Configuration
public class ElideConfiguration {
    @Bean
    public ElideOpenApiCustomizer elideOpenApiCustomizer() {
        return new MyCustomElideOpenApiCustomizer();
    }
}
```

When `GroupedOpenApi` is used, the `ElideOpenApiCustomizer` is not applied to the groups. Instead Elide has a
`DefaultElideGroupedOpenApiCustomizer` that will customize the `GroupedOpenApi` to set the appropriate
`OpenApiCustomizers` on the `GroupedOpenApi` that matches the paths to match and exclude. To override the behavior a
`ElideGroupedOpenApiCustomizer` can be defined that will need to process the `OpenApiCustomizers` and remove the ones
automatically added by Elide.

```java
@Configuration
public class ElideConfiguration {
    @Bean
    public ElideGroupedOpenApiCustomizer elideGroupedOpenApiCustomizer() {
        return new MyCustomElideGroupedOpenApiCustomizer();
    }
}
```

#### Elide Standalone Configuration

If we are using [Elide Standalone](https://github.com/paion-data/elide/tree/master/elide-standalone), we can extend
`ElideStandaloneSettings` to:

- Enable the OpenAPI document endpoint.
- Configure the URL Path for the OpenAPI document.
- Configure the OpenAPI document version.
- Configure the service name.
- Construct OpenAPI documents for your service.

```java
public abstract class Settings implements ElideStandaloneSettings {
    /**
     * Enable OpenAPI documentation.
     * @return whether OpenAPI is enabled;
     */
    @Override
    public boolean enableApiDocs() {
        return true;
    }

    /**
     * API root path specification for the OpenAPI endpoint. Namely, this is the root uri for OpenAPI docs.
     *
     * @return Default: /api-docs/*
     */
    @Override
    public String getApiDocsPathSpec() {
        return "/api-docs/*";
    }

    /**
     * OpenAPI documentation requires an API name.
     * @return open api service name;
     */
    @Override
    public String getApiTitle() {
        return "Elide Service";
    }

    /**
     * The OpenAPI Specification Version to generate.
     * @return the OpenAPI Specification Version to generate
     */
    @Override
    public OpenApiVersion getOpenApiVersion() {
        return OpenApiVersion.OPENAPI_3_0;
    }

    /**
     * Creates a singular OpenAPI document for JSON-API.
     * @param dictionary Contains the static metadata about Elide models. .
     * @return list of OpenAPI registration objects.
     */
    @Override
    public List<ApiDocsEndpoint.ApiDocsRegistration> buildApiDocs(EntityDictionary dictionary) {
        List<ApiDocsEndpoint.ApiDocsRegistration> docs = new ArrayList<>();

        dictionary.getApiVersions().stream().forEach(apiVersion -> {
            Info info = new Info()
                    .title(getApiTitle())
                    .version(apiVersion);
            OpenApiBuilder builder = new OpenApiBuilder(dictionary).apiVersion(apiVersion);
            String moduleBasePath = getJsonApiPathSpec().replace("/*", "");
            OpenAPI openApi = builder.build().info(info).addServersItem(new Server().url(moduleBasePath));
            docs.add(new ApiDocsEndpoint.ApiDocsRegistration("test", () -> openApi, getOpenApiVersion().getValue(),
                    apiVersion));
        });

        return docs;
    }
}
```

### Elide Library Configuration

If we are using Elide directly as a library (and not using Elide Standalone), follow these instructions:

Create and initialize an entity dictionary.

```java
EntityDictionary dictionary = EntityDictionary.builder().build();

dictionary.bindEntity(Book.class);
dictionary.bindEntity(Author.class);
dictionary.bindEntity(Publisher.class);
```

Create a Info object.

```java
Info info = new Info().title("My Service").version("1");
```

Initialize a OpenAPI builder.

```java
OpenApiBuilder builder = new OpenApiBuilder(dictionary);
```

Build the OpenAPI document

```java
OpenAPI document = builder.build().info(info);
```

#### Converting OpenAPI to JSON or YAML

We can directly convert to JSON:

```java
OpenApiDocument openApiDocument = new OpenApiDocument(document, OpenApiDocument.Version.from("3.0"));
String jsonOutput = openApiDocument.of(OpenApiDocument.MediaType.APPLICATION_JSON);
```

We can directly convert to YAML as well:

```java
OpenApiDocument openApiDocument = new OpenApiDocument(document, OpenApiDocument.Version.from("3.0"));
String jsonOutput = openApiDocument.of(OpenApiDocument.MediaType.APPLICATION_YAML);
```

#### Configure JAX-RS Endpoint

Or we can use the OpenAPI document directly to configure the [provided JAX-RS Endpoint](https://github.com/paion-data/elide/blob/master/elide-swagger/src/main/java/com/paiondata/elide/swagger/resources/ApiDocsEndpoint.java):

```java
List<ApiDocsEndpoint.ApiDocsRegistration> apiDocs = new ArrayList<>();
OpenAPI openApi = new OpenAPI();
apiDocs.add(new ApiDocsEndpoint.ApiDocsRegistration("test", () -> openApi, "3.0", ""));

//Dependency Inject the ApiDocsEndpoint JAX-RS resource
bind(apiDocs).named("apiDocs").to(new TypeLiteral<List<ApiDocsEndpoint.ApiDocsRegistration>>() { });
```

### Supporting OAuth

If we want Swagger UI to acquire & use a bearer token from an OAuth identity provider, we can configure
the OpenAPI document similar to:

```java
SecurityScheme oauthDef = new SecurityScheme()
    .name("bearerAuth")
    .type(SecurityScheme.Type.HTTP)
    .bearerFormat("JWT")
    .scheme("bearer");
SecurityRequirement oauthReq = new SecurityRequirement().addList("bearerAuth");

OpenApiBuilder builder = new OpenApiBuilder(entityDictionary);
OpenAPI document = builder.build();
document.addSecurityItem(oauthReq);
document.getComponents().addSecuritySchemes("bearerAuth", oauthDef);
```

### Adding a global parameter

A query or header parameter can be added globally to all Elide API endpoints:

```java
Parameter oauthParam = new Parameter()
        .in("Header")
        .name("Authorization")
        .schema(new StringSchema())
        .description("OAuth bearer token")
        .required(false);

OpenApiBuilder builder = new OpenApiBuilder(dictionary).globalParameter(oauthParam);
```

### Adding a global response code

An HTTP response can be added globally to all Elide API endpoints:

```java
ApiResponse rateLimitedResponse = new ApiResponse().description("Too Many Requests");

OpenApiBuilder builder = new OpenApiBuilder(dictionary).globalResponse(429, rateLimitedResponse);
```

Performance
-----------

### Path Generation

The Swagger UI is very slow when the number of generated URL paths exceeds a few dozen. For large, complex data models,
it is recommended to generate separate OpenAPI documents for subgraphs of the model.

```java
Set<Type<?>> entities = Set.of(
    ClassType.of(Book.class),
    ClassType.of(Author.class),
    ClassType.of(Publisher.class)
);

OpenApiBuilder builder = new OpenApiBuilder(dictionary).managedClasses(entities);
```

In the example above, the `OpenApiBuilder` will only generate paths that exclusively traverse the provided set of
entities.

### Document Size

The size of the OpenAPI document can be reduced significantly by limiting the number of filter operators that are used
to generate query parameter documentation.

In this example, filter query parameters are only generated for the _IN_ operator.

```java
OpenApiBuilder builder = new OpenApiBuilder(dictionary).filterOperators(Set.of(Operator.IN));
```

### Model Properties

Elide extracts the model description and title from the `Schema` and `Include` annotations and adds them to the OpenAPI
documentation. `Schema` has precedence over `Include` if both are present.

```java
@Schema(description = "A book model description", title = "Book")
class Book {

}
```

For `Schema` only the _description_ and _title_ property is extracted. For the `Include` annotation, the _friendlyName_
is used as the _title_.

### Attribute Properties

Elide extracts properties from the `Schema` annotation and adds them to the OpenAPI documentation.

```java
class Book {

    @Schema(requiredMode = RequiredMode.REQUIRED)
    public String title;
}
```

Only the _required_, _value_, _example_, _readOnly_, _writeOnly_, _requiredProperties_, _requiredMode_ and _accessMode_
properties are extracted. This is currently only supported for attributes on Elide models.

API Versions
------------

OpenAPI documents are tied to an explicit API version. When constructing a OpenAPI document, the API version must be set
to match the API version of the models it will describe:

```java
OpenApiBuilder builder = new OpenApiBuilder(dictionary).apiVersion("1");
OpenAPI openApi = builder.build();
```

[elide-spring]: https://github.com/paion-data/elide/tree/master/elide-spring/elide-spring-boot-autoconfigure
[elide-standalone]: https://github.com/paion-data/elide/tree/master/elide-standalone
