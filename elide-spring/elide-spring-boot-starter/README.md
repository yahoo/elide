# Elide Spring Boot Starter

For more information on Elide, visit [elide.io](https://elide.io).

Opinionated jar which packages dependencies to get started with Elide and Spring Boot.  The starter includes:
1. Spring Boot Web
2. Spring Boot JPA
3. YAML Configuration
4. All of the Elide dependencies

## Maven Dependency

```xml
<dependency>
    <groupId>com.yahoo.elide</groupId>
    <artifactId>elide-spring-boot-starter</artifactId>
    <version>${elide.version}</version>
</dependency>
```

## Example Usage.

An example project can be viewed [here](https://github.com/yahoo/elide-spring-boot-example).

## Configuration

Elide can be configured in `application.yaml` with settings like this:

```yaml
elide:
  default-page-size: 1000
  max-page-size: 10000
  json-api:
    path: /json
    enabled: true
  graphql:
    path: /graphql
    enabled: true
  api-docs:
    path: /doc
    enabled: true
```

For more information on custom configuration, see the [configuration documentation](https://elide.io/pages/guide/v7/17-configuration.html).


## Dependencies

### Excludable Dependencies

The following dependencies are automatically included, but can be explicitly excluded if they are not required.

```xml
<dependency>
    <groupId>com.yahoo.elide</groupId>
    <artifactId>elide-spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>com.yahoo.elide</groupId>
            <artifactId>elide-async</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.yahoo.elide</groupId>
            <artifactId>elide-datastore-aggregation</artifactId>
        </exclusion>				
        <exclusion>
            <groupId>com.yahoo.elide</groupId>
            <artifactId>elide-datastore-jms</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.yahoo.elide</groupId>
            <artifactId>elide-graphql</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.yahoo.elide</groupId>
            <artifactId>elide-swagger</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

## License
This project is licensed under the terms of the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) open source license.
