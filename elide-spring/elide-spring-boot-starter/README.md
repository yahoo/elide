# Elide Spring Boot Starter

Opinionated jar which packages dependencies to get started with Elide and Spring Boot.  The starter includes:
1. Spring Boot Web (minus tomcat)
2. Spring Boot Jetty
3. Spring Boot JPA
4. YAML Configuration
5. All of the Elide dependencies

## Maven Dependency

```xml
   <dependency>
       <groupId>com.yahoo.elide</groupId>
       <artifactId>elide-spring-boot-starter</artifactId>
       <version>${elide.version}</version>
   </dependency>
```

## Example Usage.

An example project can be viewed [here](https://github.com/aklish/elide-spring).

## Configuration

Elide can be configured in `application.yaml` with settings like this:

```yaml
elide:
  pageSize: 1000
  maxPageSize: 10000
  json-api:
    path: /json
    enabled: true
  graphql:
    path: /graphql
    enabled: true
  swagger:
    path: /doc
    enabled: true
    name: 'My Awesome Service'
    version: "1.0"
```

For more information on custom configuration, see the [elide-spring-boot-autoconfigure documentation](https://github.com/yahoo/elide/blob/master/elide-spring/elide-spring-boot-autoconfigure/README.md).
