# Elide

> _Opinionated APIs for web & mobile applications._

![Elide Logo](elide-logo.svg)

[![Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/elide)
[![Build Status](https://travis-ci.org/yahoo/elide.svg?branch=master)](https://travis-ci.org/yahoo/elide)
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

[Elide](http://elide.io/) is a Java library that lets you set up a [GraphQL](http://graphql.org) or [JSON API](http://jsonapi.org) web service with minimal effort starting from
a [JPA annotated data model](https://en.wikipedia.org/wiki/Java_Persistence_API).

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

### Tutorials
[Create a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/create-a-json-api-rest-service-with-spring-boot-an)

[Custom Security With a Spring Boot/Elide Json API Server](https://dzone.com/articles/custom-security-with-a-spring-bootelide-json-api-s)

[Logging Into a Spring Boot/Elide JSON API Server](https://dzone.com/articles/logging-into-a-spring-bootelide-json-api-server)

[Securing a JSON API REST Service With Spring Boot and Elide](https://dzone.com/articles/securing-a-json-api-rest-service-with-spring-boot)

[Creating Entities in a Spring Boot/Elide JSON API Server](https://dzone.com/articles/creating-entities-in-a-spring-bootelide-json-api-s)

[Updating and Deleting with a Spring Boot/Elide JSON API Server](https://dzone.com/articles/updating-and-deleting-with-a-spring-bootelide-json)

## Install

To try out an Elide example service (with a Postgres database), you can deploy via Heroku.

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/yahoo/elide)

The code that generates this example can be found [here](https://github.com/yahoo/elide/tree/master/elide-example/elide-blog-example)

Alternatively, use [elide-standalone](https://github.com/yahoo/elide/tree/master/elide-standalone) which allows you to quickly setup a local instance
of Elide running inside an embedded Jetty application.

## Usage

To use Elide, create a set of JPA annotated data models that represent the domain model of your web service:

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
@DeletePermission("Noone"
@UpdatePermission("Noone")
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
@DeletePermission("Noone")
@UpdatePermission("Noone")
public class Book {

    @Id
    private Integer id;

    @UpdatePermission("Admin OR Publisher")
    private String title;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors;

    @OnCreatePreCommit
    public void onCreate(RequestScope scope) {
       //Do something
    }
}
```

Map expressions to security functions or predicates that get pushed to the persistence layer:

```java
    public static class IsAdminUser extends UserCheck {
        @Override
        public boolean ok(User user) {
            return isUserInRole(user, UserRole.admin);
        }
    }
```

To expose these models, follow the steps documented in [elide-standalone](https://github.com/yahoo/elide/tree/master/elide-standalone):

```java
public class YourMain {
    public static void main(String[] args) {

        ElideStandaloneSettings settings = new ElideStandaloneSettings() {

            @Override
            public String getModelPackageName() {
                //This needs to be changed to the package where your models live.
                return "your.model.package";
            }

            @Override
            public Map<String, Class<? extends Check>> getCheckMappings() {
                //Maps expression clauses to your security check functions & predicates
                return new HashMap<String, Class<? extends Check>>() { {
                    put("Admin", IsAdminUser.class);
                } };
            }
        };

        ElideStandalone elide = new ElideStandalone(settings);

        elide.start();
    }
}
```

For example API calls, look at:
1. [*JSON-API*](http://elide.io/pages/guide/10-jsonapi.html)
2. [*GraphQL*](http://elide.io/pages/guide/11-graphql.html)

## Security

Security is documented in depth [here](http://elide.io/pages/guide/03-security.html).

## Contribute
Please refer to [the contributing.md file](CONTRIBUTING.md) for information about how to get involved. We welcome issues, questions, and pull requests.

If you are contributing to Elide using an IDE, such as IntelliJ, make sure to install the [Lombok](https://projectlombok.org/) plugin.

Discussion is on [spectrum](https://spectrum.chat/elide) or through filing issues.

## License
This project is licensed under the terms of the [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) open source license.
Please refer to [LICENSE](LICENSE.txt) for the full terms.
