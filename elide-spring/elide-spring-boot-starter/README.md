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
       <groupId>com.paiondata.elide</groupId>
       <artifactId>elide-spring-boot-starter</artifactId>
       <version>${elide.version}</version>
   </dependency>
```

## Example Usage

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

For more information on custom configuration, see the [elide-spring-boot-autoconfigure documentation](https://github.com/paion-data/elide/blob/master/elide-spring/elide-spring-boot-autoconfigure/README.md).

## License
This project is licensed under the terms of the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) open source license.
