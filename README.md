[![Build Status](https://travis-ci.org/yahoo/elide.svg?branch=master)](https://travis-ci.org/yahoo/elide) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core)

![Elide Logo](https://cdn.rawgit.com/yahoo/elide/master/elide.svg)

##What Is Elide?

Elide is a Java library that let's you stand up a [JSON API](http://jsonapi.org) web service with minimal effort starting from a JPA annotated data model. 
Elide is designed to quickly build and deploy **production quality** web services that expose databases as services.  Beyond the basics, elide provides:
  1. **Access** to JPA entities via JSON API CRUD operations.  Entities can be explicitly included or excluded via annotations.
  2. **Patch Extension** Elide supports the [JSON API Patch extension](http://jsonapi.org/extensions/jsonpatch/) allowing multiple create, edit, and delete operations in a single request.
  3. **Atomic Requests** All requests to the library (including the patch extension) can be embedded in transactions to ensure operational integrity.
  4. **Authorization** All operations on entities and their fields can be assigned custom permission checks limiting who has access to your data. 
  5. **Audit** Logging can be customized for any operation on any entity.
  6. **Extension** Elide allows the ability to add custom business logic and replaceable JPA provider (from the default Hibernate provider).
  7. **Client API** Elide is developed in conjunction with a Javascript client library that insulates developers from changes to the specification.

##Documentation

More information about Elide can be found at [elide.io](http://elide.io/).

##Getting Started

### Using Elide

To integrate Elide into your project, simply include elide-core into your project's pom.xml:

```xml
<!-- Elide -->
<dependency>
    <groupId>com.yahoo.elide</groupId>
    <artifactId>elide-core</artifactId>
    <version>1.0.0.12</version>
</dependency>
```

Additionally, if you do not plan to write your own data store, select the appropriate data store for your setup and include it as well. For instance, if you plan on using the "in-memory database" (not recommended for production use) then you would add the following:

```xml
<dependency>
    <groupId>com.yahoo.elide</groupId>
    <artifactId>elide-datastore-inmemorydb</artifactId>
    <version>1.0.0.12</version>
</dependency>
```

###Code 

The first step is to create a JPA data model and mark which beans to expose via Elide.  The following directive exposes **everything** in a package:  

    @Include(rootLevel=true)
    package example;

The second step is to create a `DataStore`.   It is an interface that binds to a JPA provider.  Elide ships with a default implementation for
Hibernate.  The default `HibernateStore` will discover all of the JPA beans in your deployment and expose those that have been annotated to do so.

    /* Takes a hibernate session factory */
    DataStore dataStore = new HibernateStore(sessionFactory);

The third step is to create an `AuditLogger`.   It is an interface that does something with Audit messages.  Elide ships with a default that
dumps them to slf4j:

    AuditLogger logger = new Slf4jLogger();

Create an `Elide class`.  It is the entry point for handling requests from your web server/container.  

    Elide elide = new Elide(logger, dataStore);

`Elide` has methods for `get`, `patch`, `post`, and `delete`.  These methods generally take:
  1. An opaque user `Object`
  2. An HTTP path as a `String`
  3. A JSON API document as a `String` representing the request entity body (if one is required).

It returns a `ElideResponse` which contains the HTTP response status code and a `String` which contains the response entity body.

    ElideResponse response = elide.post(path, requestBody, user)

Wire up the four HTTP verbs to your container and you will have a functioning JSON API server.

##License

The use and distribution terms for this software are covered by the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html).
