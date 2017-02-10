[![Gitter](https://badges.gitter.im/yahoo/elide.svg)](https://gitter.im/yahoo/elide?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) [![Build Status](https://travis-ci.org/yahoo/elide.svg?branch=master)](https://travis-ci.org/yahoo/elide) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core)

![Elide Logo](https://cdn.rawgit.com/yahoo/elide/master/elide.svg)

##What Is Elide?

Elide is a Java library that lets you stand up a [JSON API](http://jsonapi.org) web service with minimal effort starting from a [JPA annotated data model](https://en.wikipedia.org/wiki/Java_Persistence_API). 
Elide is designed to quickly build and deploy **production quality** web services that expose data models as services.  Elide provides:
  1. **Access** to JPA entities via JSON API CRUD operations.  Entities can be explicitly included or excluded via annotations.
  2. **Patch Extension** Elide supports the [JSON API Patch extension](http://jsonapi.org/extensions/jsonpatch/) allowing multiple create, edit, and delete operations in a single request.
  3. **Atomic Requests** All requests to the library (including the patch extension) can be embedded in transactions to ensure operational integrity.
  4. **Authorization** All operations on entities and their fields can be assigned custom permission checks limiting who has access to your data. 
  5. **Audit** Logging can be customized for any operation on any entity.
  6. **Extension** Elide allows the ability to customize business logic for any CRUD operation on the model.  Any persistence backend can be skinned with JSON-API by wiring in a JPA provider or by implementing a custom `DataStore`.
  7. **Test** Elide includes a test framework that explores the full surface of the API looking for security vulnerabilities.
  8. **Client API** Elide is developed in conjunction with a Javascript client library that insulates developers from changes to the specification.  Alternatively, Elide can be used with any [JSON API client library](http://jsonapi.org/implementations/).

##Documentation

More information about Elide can be found at [elide.io](http://elide.io/).

##Elide on Maven

To integrate Elide into your project, simply include elide-core into your project's pom.xml:

```xml
<!-- Elide -->
<dependency>
    <groupId>com.yahoo.elide</groupId>
    <artifactId>elide-core</artifactId>
</dependency>
```

Additionally, if you do not plan to write your own data store, select the appropriate data store for your setup and include it as well. For instance, if you plan on using the "in-memory database" (not recommended for production use) then you would add the following:

```xml
<dependency>
    <groupId>com.yahoo.elide</groupId>
    <artifactId>elide-datastore-inmemorydb</artifactId>
</dependency>
```

## Development

If you are contributing to Elide using IDE, such as IntelliJ, make sure to install [Lombok](https://projectlombok.org/) plugin.

##Tutorials
[Create a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/create-a-json-api-rest-service-with-spring-boot-an)

[Custom Security With a Spring Boot/Elide Json API Server](https://dzone.com/articles/custom-security-with-a-spring-bootelide-json-api-s)

[Logging Into a Spring Boot/Elide JSON API Server](https://dzone.com/articles/logging-into-a-spring-bootelide-json-api-server)

[Securing a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/securing-a-json-api-rest-service-with-spring-boot)

[Creating Entities in a Spring Boot/Elide JSON API Server](https://dzone.com/articles/creating-entities-in-a-spring-bootelide-json-api-s)

[Updating and Deleting with a Spring Boot/Elide JSON API Server](https://dzone.com/articles/updating-and-deleting-with-a-spring-bootelide-json)

##License

The use and distribution terms for this software are covered by the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html).
