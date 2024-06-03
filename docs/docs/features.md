---
sidebar_position: 3
title: Features
description: An overview of Elide features and links to their respective documentations.
---

The following guide provides an overview of Elide features and links to their respective documentation.

<!--truncate-->

Common API Features
-------------------

- **Rich Filter Support** - Support for complex filter predicates including conjunction (logical and), disjunction
  (logical OR) and parenthetic expressions for both [GraphQL](graphql#filtering) and [JSON-API](jsonapi#filtering).
  Support for filtering models on fields nested in other models (relationship traversal) or attribute object
  hierarchies (complex attribute types).
- **Collection Sorting** - Sort collections by one or more fields in the current or related models in
  [GraphQL](graphql#sorting) and [JSON-API](jsonapi#sorting).
- **Pagination** - Support to paginate collections and request the total number of pages or records in
  [GraphQL](graphql#pagination) and [JSON-API](jsonapi#pagination).
- **Type Coercion** - Support to [type coerce](clientapis#type-coercion) fields between the API representation and
  the model representation by registering one's own custom data type serializers/deserializers.
- **Synchronous or Asynchronous API** - Elide supports both synchronous and asynchronous APIs for short and long-running
  queries.

Data Modeling Features
----------------------

- **Lifecycle Hooks** - Register [custom functions & business logic](data-model#lifecycle-hooks) that get invoked
  whenever our data model is read or manipulated.
- **Security** - Assign [permission rules](security) to fields and entities in our data model using a custom security
  DSL.  Bind rules to in-memory functions or security filters that are pushed to the persistence layer.
- **Computed Attributes & Relationships** - Define [custom fields and relationships](data-model#computed-attributes)
  that are computed at query time.
- **API Versioning** - [Version our models](data-model#api-versions) to support schema evolution without breaking our
  client contracts.
- **Composite Identifiers** - Support both simple and complex [model identifiers](data-model#model-identifiers)
  including compound types.

JSON-API Features
-----------------

- **OpenAPI** - Elide can automatically generate [OpenAPI documentation](openapi) for Elide APIS for schema
  introspection.
- **Test DSL** - Elide includes a [test DSL](test) that works with [Rest Assured](https://rest-assured.io/) for writing
  readable integration tests.

GraphQL Features
----------------

- **GraphQL Schemas** - The GraphQL specification includes type introspection that integrates with tools like [Graphiql](https://github.com/graphql/graphiql).
- **Subscription Suport** - Elide supports [model driven subscriptions](subscriptions) backed by any JMS message broker
  that can be consumed over websockets.
- **Test DSL** - Elide includes a [test DSL](test) that works with [Rest Assured](https://rest-assured.io/) for
  writing readable integration tests.

Persistence Features
--------------------

- **JPA Store** - The [JPA store](datastores#jpa-store) persists Elide models decorated with JPA annotations.
- **In-Memory Store** - The [in-memory store](datastores#in-memory-store) persists Elide models locally in the server's
  memory.
- **Search Store** - The [search store](datastores#search-store) provides full text search on annotated fields in Elide
  models.  It works in conjunction with the JPA store.
- **Multiple Stores** - Elide services can be configured with [multiple data stores](datastores#multiple-stores) - each
  managing a different set of models.
- **Custom Stores** - Elide can be extended to talk to web services or other persistence layers by writing
  [custom stores](datastores#custom-stores).
- **Server Side Filtering, Sorting, & Pagination** - For custom stores that cannot filter, sort, or paginate natively,
  Elide can optionally perform these functions on the server.

Analytic Features
-----------------

- **Analytic Query Support** - Elide's [aggregation store](analytics) exposes read-only models that support data
  analytic queries.  Model attributes represent either metrics (for aggregating, filtering, and sorting) and
  dimensions (for grouping, filtering, and sorting).
- **Virtual Semantic Layer** - Analytic models are configured with a
  [semantic modeling language](analytics.html#model-configuration) that allows non-developers the ability to define
  metrics and dimensions by writing templated native SQL fragments. The fragments are assembled into complete SQL
  statements at query time.
- **Caching** - The aggregation store includes a [customizable cache](performance#aggregationdatastore-cache). The
  cache supports time and version based strategies for expunging stale data.
- **Async API** - Elide includes an asynchronous API for long-running queries on analytic models.
- **Data Export** - Elide includes a data export API for streaming large query results in JSON or CSV formats.

Operability Features
--------------------

- **Logging** - Elide supports rich [native logging](audit) for query generation and security rules.
- **Spring Integration** - Elide integrates with [Spring Boot](https://spring.io/projects/spring-boot) including an
  [example project](https://github.com/paion-data/elide-spring-boot-example) and
  [starter package](https://github.com/paion-data/elide/tree/master/elide-spring).
- **Elide Standalone Integration** - Elide includes
  [JAX-RS](https://download.oracle.com/otndocs/jcp/jaxrs-2_0-fr-eval-spec/index.html) endpoints and can run as a
  [standalone](https://github.com/paion-data/elide-standalone-example) embedded Jetty service.
- **Java 17** - Elide compiles with Java 17.
