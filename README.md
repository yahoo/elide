[![Gitter](https://badges.gitter.im/yahoo/elide.svg)](https://gitter.im/yahoo/elide?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) 
[![Build Status](https://travis-ci.org/yahoo/elide.svg?branch=master)](https://travis-ci.org/yahoo/elide) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core)
[![Coverage Status](https://coveralls.io/repos/github/yahoo/elide/badge.svg?branch=master)](https://coveralls.io/github/yahoo/elide?branch=master)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/yahoo/elide.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/yahoo/elide/context:java)
[![Total Alerts](https://img.shields.io/lgtm/alerts/g/yahoo/elide.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/yahoo/elide/alerts)

![Elide Logo](http://elide.io/assets//images/elide-logo.svg)

*Read this in other languages: [中文](./README-zh.md).*

## What Is Elide?

[Elide](http://elide.io/) provides opinionated APIs for web & mobile applications.  Elide is a Java library that lets you set up a [GraphQL](http://graphql.org) or [JSON API](http://jsonapi.org) web service with minimal effort starting from 
a [JPA annotated data model](https://en.wikipedia.org/wiki/Java_Persistence_API).    

### Security Comes Standard
Control access to fields and entities through a declarative, intuitive permission syntax.

### Mobile Friendly APIs
JSON-API & GraphQL lets developers fetch entire object graphs in a single round trip. Only requested elements of the data model are returned. 
Our opinionated approach for mutations addresses common application scenarios:
* Create a new object and add it to an existing collection in the same operation.
* Create a set of related, composite objects (a subgraph) and connect it to an existing, persisted graph.
* Differentiate between deleting an object vs disassociating an object from a relationship (but not deleting it).
* Change the composition of a relationship to something different.
* Reference a newly created object inside other mutation operations.

Filtering, sorting, and pagination are supported out of the box.

### Atomicity For Complex Writes
Elide supports multiple data model mutations in a single request in either JSON-API or GraphQL. Create objects, add them to relationships, modify or delete together in a single atomic request.

### Persistence Layer Agnostic
Elide is agnostic to your particular persistence strategy. Use an ORM or provide your own implementation of a data store.

### Schema Introspection
Explore, understand, and compose queries against your Elide API through generated Swagger documentation or GraphQL schema.

### Customize 
Customize the behavior of data model operations with computed attributes, data validation annotations, and request lifecycle hooks.

## Documentation

More information about Elide can be found at [elide.io](http://elide.io/).

## Development

If you are contributing to Elide using an IDE, such as IntelliJ, make sure to install the [Lombok](https://projectlombok.org/) plugin.

## Tutorials
[Create a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/create-a-json-api-rest-service-with-spring-boot-an)

[Custom Security With a Spring Boot/Elide Json API Server](https://dzone.com/articles/custom-security-with-a-spring-bootelide-json-api-s)

[Logging Into a Spring Boot/Elide JSON API Server](https://dzone.com/articles/logging-into-a-spring-bootelide-json-api-server)

[Securing a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/securing-a-json-api-rest-service-with-spring-boot)

[Creating Entities in a Spring Boot/Elide JSON API Server](https://dzone.com/articles/creating-entities-in-a-spring-bootelide-json-api-s)

[Updating and Deleting with a Spring Boot/Elide JSON API Server](https://dzone.com/articles/updating-and-deleting-with-a-spring-bootelide-json)

## License

Elide is 100% open source and released under the commercial-friendly Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html).
