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

See the [elide-spring-boot-autoconfigure documentation](https://github.com/yahoo/elide/blob/master/elide-spring/elide-spring-boot-autoconfigure/README.md) to configure your app.
