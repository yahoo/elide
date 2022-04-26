# Elide

> _Opinionated APIs for web & mobile applications._

![Elide Logo](elide-logo.svg)

[![Discord](https://img.shields.io/discord/869678398241398854)](https://discord.com/widget?id=869678398241398854&theme=dark)
[![Build Status](https://cd.screwdriver.cd/pipelines/6103/badge)](https://cd.screwdriver.cd/pipelines/6103)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.yahoo.elide/elide-core)
[![Coverage Status](https://coveralls.io/repos/github/yahoo/elide/badge.svg?branch=master)](https://coveralls.io/github/yahoo/elide?branch=master)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/yahoo/elide.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/yahoo/elide/context:java)
[![Total Alerts](https://img.shields.io/lgtm/alerts/g/yahoo/elide.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/yahoo/elide/alerts)
[![Mentioned in Awesome Java](https://awesome.re/mentioned-badge.svg)](https://github.com/akullpp/awesome-java)
[![Mentioned in Awesome GraphQL](https://awesome.re/mentioned-badge.svg)](https://github.com/chentsulin/awesome-graphql)

*Read this in other languages: [中文](translations/zh/README.md).*

## Table of Contents

- [Background](#background)
- [Documentation](#documentation)
- [Install](#install)
- [Usage](#usage)
- [Security](#security)
- [Contribute](#contribute)
- [License](#license)

## Background

[Elide](https://elide.io/) is a Java library that lets you setup model driven [GraphQL](http://graphql.org) or [JSON API](http://jsonapi.org) web service with minimal effort.  Elide supports two variants of APIs:

1. A CRUD (Create, Read, Update, Delete) API for reading and manipulating models.
2. An analytic API for aggregating measures over zero or more model attributes.

Elide supports a number of features:

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

Filtering, sorting, pagination, and text search are supported out of the box.

### Atomicity For Complex Writes
Elide supports multiple data model mutations in a single request in either JSON-API or GraphQL. Create objects, add them to relationships, modify or delete together in a single atomic request.

## Analytic Query Support
Elide supports analytic queries against models crafted with its powerful semantic layer.  Elide APIs work natively with [Yavin](https://github.com/yahoo/yavin) to visualize, explore, and report on your data.

### Schema Introspection
Explore, understand, and compose queries against your Elide API through generated Swagger documentation or GraphQL schema.

### Customize
Customize the behavior of data model operations with computed attributes, data validation annotations, and request lifecycle hooks.

### Storage Agnostic
Elide is agnostic to your particular persistence strategy. Use an ORM or provide your own implementation of a data store.

## Documentation

More information about Elide can be found at [elide.io](https://elide.io/).

## Install

To try out an Elide example service, check out this [Spring boot example project](https://github.com/yahoo/elide-spring-boot-example).

Alternatively, use [elide-standalone](https://github.com/yahoo/elide/tree/master/elide-standalone) which allows you to quickly setup a local instance of Elide running inside an embedded Jetty application.

## Usage 

### For CRUD APIs

The simplest way to use Elide is by leveraging [JPA](https://en.wikipedia.org/wiki/Java_Persistence_API) to map your Elide models to persistence:

The models should represent the domain model of your web service:

```java
@Entity
public class Book {

    @Id
    private Integer id;

    private String title;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors;
}
```

Add Elide annotations to both expose your models through the web service and define security policies for access:

```java
@Entity
@Include(rootLevel = true)
@ReadPermission("Everyone")
@CreatePermission("Admin OR Publisher")
@DeletePermission("None")
@UpdatePermission("None")
public class Book {

    @Id
    private Integer id;

    @UpdatePermission("Admin OR Publisher")
    private String title;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors;
}
```

Add Lifecycle hooks to your models to embed custom business logic that execute inline with CRUD operations through the web service:

```java
@Entity
@Include(rootLevel = true)
@ReadPermission("Everyone")
@CreatePermission("Admin OR Publisher")
@DeletePermission("None")
@UpdatePermission("None")
@LifeCycleHookBinding(operation = UPDATE, hook = BookCreationHook.class, phase = PRECOMMIT)
public class Book {

    @Id
    private Integer id;

    @UpdatePermission("Admin OR Publisher")
    private String title;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors;
}

public class BookCreationHook implements LifeCycleHook<Book> {
    @Override
    public void execute(LifeCycleHookBinding.Operation operation,
                        LifeCycleHookBinding.TransactionPhase phase,
                        Book book,
                        RequestScope requestScope,
                        Optional<ChangeSpec> changes) {
       //Do something
    }
}

```

Map expressions to security functions or predicates that get pushed to the persistence layer:

```java
    @SecurityCheck("Admin")
    public static class IsAdminUser extends UserCheck {
        @Override
        public boolean ok(User user) {
            return isUserInRole(user, UserRole.admin);
        }
    }
```

To expose and query these models, follow the steps documented in [the getting started guide](https://elide.io/pages/guide/v5/01-start.html).

For example API calls, look at:
1. [*JSON-API*](https://elide.io/pages/guide/v5/10-jsonapi.html) 
2. [*GraphQL*](https://elide.io/pages/guide/v5/11-graphql.html)

### For Analytic APIs

Analytic models including tables, measures, dimensions, and joins can be created either as POJOs or with a friendly HJSON configuration language:

```hjson
{
  tables: [
    {
      name: Orders
      table: order_details
      measures: [
        {
          name: orderTotal
          type: DECIMAL
          definition: 'SUM({{$order_total}})'
        }
      ]
      dimensions: [
        {
          name: orderId
          type: TEXT
          definition: '{{$order_id}}'
        }
      ]
    }
  ]
}
```

More information on configuring or querying analytic models can be found [here](https://elide.io/pages/guide/v5/04-analytics.html).

## Security

Security is documented in depth [here](https://elide.io/pages/guide/v5/03-security.html).

## Contribute
Please refer to [the contributing.md file](CONTRIBUTING.md) for information about how to get involved. We welcome issues, questions, and pull requests.

If you are contributing to Elide using an IDE, such as IntelliJ, make sure to install the [Lombok](https://projectlombok.org/) plugin.

Community chat is now on [discord](https://discord.com/widget?id=869678398241398854&theme=dark).  Join by clicking [here](https://discord.gg/3vh8ac57cc).
Legacy discussion is archived on [spectrum](https://spectrum.chat/elide).

## License
This project is licensed under the terms of the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) open source license.
Please refer to [LICENSE](LICENSE.txt) for the full terms.

## Articles
Intro to Elide video

[![Intro to Elide](http://img.youtube.com/vi/WeFzseAKbzs/0.jpg)](http://www.youtube.com/watch?v=WeFzseAKbzs "Intro to Elide")

[Create a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/create-a-json-api-rest-service-with-spring-boot-an)

[Custom Security With a Spring Boot/Elide Json API Server](https://dzone.com/articles/custom-security-with-a-spring-bootelide-json-api-s)

[Logging Into a Spring Boot/Elide JSON API Server](https://dzone.com/articles/logging-into-a-spring-bootelide-json-api-server)

[Securing a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/securing-a-json-api-rest-service-with-spring-boot)

[Creating Entities in a Spring Boot/Elide JSON API Server](https://dzone.com/articles/creating-entities-in-a-spring-bootelide-json-api-s)

[Updating and Deleting with a Spring Boot/Elide JSON API Server](https://dzone.com/articles/updating-and-deleting-with-a-spring-bootelide-json)
