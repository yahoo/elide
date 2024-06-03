---
sidebar_position: 4
title: Data Models
description: Creating CRUD data models (create, read, update, and delete) in the backend using Elide
---

:::note

This page is a description on how to _create_ CRUD data models (create, read, update, and delete) in the backend
using Elide. For more information on _interacting_ with an Elide API, please see the
[API usage documentation](clientapis). More information for creating analytic models can be found [here](analytics).

:::

Elide generates its API entirely based on the concept of **data models**. Data models are JVM classes that represent
both a concept to our application and the _schema_ of an exposed web service endpoint. Data models are intended to be a
_view_ on top of the [data store](datastores) or the set of data stores which support our Elide-based service.

All Elide models have an identifier field that identifies a unique instance of the model. Models are also composed of
optional attributes and relationships. **Attribute** are properties of the model. **Relationships** are simply links to
other related Elide models. Annotations are used to declare that a class is an Elide model, that a relationship exists
between two models, to denote which field is the identifier field, and to [secure the model](security).

Annotations
-----------

Elide has first class support for
[JPA (Java Persistence API)](http://www.oracle.com/technetwork/java/javaee/tech/persistence-jsp-140049.html)
annotations. These annotations serve double duty by both:

- describing the attributes, relationships, and id field of a model.
- provide an object relational mapping that can be used by an Elide data store to persist the model.

Elide makes use of the following JPA annotations: `@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@Id`,
`@EmbeddedId`, and `@GeneratedValue`.

If more information about JPA is need, please
[review their documentation](http://www.oracle.com/technetwork/java/javaee/tech/persistence-jsp-140049.html) or see
our examples below.

However, JPA is not required and Elide supports its own set of annotations for describing models:

| Annotation Purpose       | JPA                         | Non-JPA           |
|--------------------------|-----------------------------|-------------------|
| Expose a model in elide  |                             | `@Include`        |
| To One Relationship      | `@OneToOne`, `@ManyToOne`   | `@ToOne`          |
| To Many Relationship     | `@OneToMany`, `@ManyToMany` | `@ToMany`         |
| Mark an identifier field | `@Id`, `@EmbeddedId`        |                   |

Much of the Elide per-model configuration is done via annotations. For a full description of all
Elide-supported annotations, please check out the annotation Javadoc.

Exposing a Model as an Elide Endpoint
-------------------------------------

After creating a proper data model, we can expose it through Elide by marking with with `@Include`. Elide
generates its API as a _graph_. This graph can only be traversed starting at a _root_ node. Rootable entities are
denoted by applying `@Include` to the top-level of the class with the **rootLevel** property unset or set to `true`.
Non-rootable entities can be accessed only as relationships through the graph.

```java
@Entity
@Include
public class Author {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;

    private String name;

    @ManyToMany
    private Set<Book> books;
}
```

```java
@Entity
@Include(rootLevel = false)
public class Book {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;

    private String title;

    @ManyToMany
    private Set<Author> authors;
}
```

Considering the example above, we have a full data model that exposes a specific graph. Namely, a root node of the
type `Author` and a bi-directional relationship from `Author` to `Book`. That is, one can access all `Author`
objects directly, but must go _through_ an author to see information about any specific `Book` object.

Model Identifiers
-----------------

Every model in Elide must have an ID. This is a requirement of both the JSON-API specification and Elide's GraphQL API.
Identifiers can be assigned by the persistence layer automatically or the client. Elide must know two things:

1. What field is the ID of the model. This is determined by the `@Id` or `@EmbeddedId` annotation.
2. Whether the persistence layer is assigning the ID or not. This is determined by the presence or absence of the
   `@GeneratedValue` annotation.

Identifier fields in Elide are typically integers, longs, strings, or UUIDs. It is also possible to have
composite/compound ID fields composed of multiple fields. For example, the following identifier type includes three
fields that together create a primary key:

```java
@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Address implements Serializable {
    private long number;
    private String street;
    private long zipCode;
}
```

This new compound ID type can then be referenced in an Elide model identifier like this:

```java
@Data
@Entity
@Include(rootLevel = true)
public class Building {
    @EmbeddedId
    private Address address;
}
```

Because JSON-API requires all ID fields to be Strings, composite/compound IDs require the developer to register an
Elide `Serde` to serialize and deserialize the ID type to a String. For example, the following `Serde` will
encode/decode an `Address` as a base64 encoded string:

```java
@ElideTypeConverter(type = Address.class, name = "Address")
public class AddressSerde implements Serde<String, Address> {

    private static final Pattern ADDRESS_PATTERN =
            Pattern.compile("Address\\(number=(\\d+), street=([a-zA-Z0-9 ]+), zipCode=(\\d+)\\)");

    @Override
    public Address deserialize(String val) {
        byte[] decodedBytes = Base64.getDecoder().decode(val);
        String decodedString = new String(decodedBytes);

        Matcher matcher = ADDRESS_PATTERN.matcher(decodedString);
        if (! matcher.matches()) {
            throw new InvalidValueException(decodedString);
        }
        long number = Long.valueOf(matcher.group(1));
        String street = matcher.group(2);
        long zipCode = Long.valueOf(matcher.group(3));

        Address address = new Address(number, street, zipCode);

        return address;
    }

    @Override
    public String serialize(Address val) {
        return Base64.getEncoder().encodeToString(val.toString().getBytes());
    }
}
```

More information about `Serde` and user defined types can be found [here](clientapis#type-coercion).

Attributes vs Relationships
---------------------------

Elide distinguishes between attributes and relationships in a data model:

1. *Relationships* are links from one model to another. They can be traversed directly through the API. If the
   relationship represents a collection, they can also be sorted, filtered, and paginated. Relationships must be
   explicitly marked with an annotation (for example - `@ToMany`) in the model and can be bidirectional or
   unidirectional.
2. *Attributes* are properties of a model. They can be primitive types, objects, or collections of objects or
   primitives. Attributes which are collections cannot be sorted, filtered, or paginated in the API. Complex
   attributes (collections or objects) cannot be used in a filter predicate. Attributes are not marked with
   annotations in Elide.

Model Properties or Fields
--------------------------

An Elide model can be described using properties (getter and setter functions) or fields (class member variables) but
_not_ both on the same entity. For any given entity, Elide looks at whether `@Id` or `@EmbeddedId` is a property or
field to determine the access mode (property or field) for that entity. All public properties and all fields are
exposed through the Elide API if they are not explicitly marked `@Transient` or `@Exclude`. `@Transient` allows a field
to be ignored by both Elide and an underlying persistence store while `@Exclude` allows a field to exist in the
underlying persistence layer without exposing it through the Elide API.

Computed Attributes
-------------------

A computed attribute is an entity attribute whose value is computed in code rather than fetched from a data store.

Elide supports computed properties by way of the `@ComputedAttribute` and `@ComputedRelationship` annotations. These
are useful if our data store is also tied to your Elide view data model. For instance, if we mark a field `@Transient`,
a data store such as Hibernate will ignore it. In the absence of the `@Computed*` attributes, Elide will too. However,
when applying a computed property attribute, Elide will expose this field anyway.

A computed attribute can perform arbitrary computation and is exposed through Elide as a typical attribute. In the case
below, this will create an attribute called `myComputedAttribute`.

```java
@Entity
@Include
public class Book {

    ...

    @Transient
    @ComputedAttribute
    public String getMyComputedAttribute(RequestScope requestScope) {
        return "My special string stored only in the JVM!";
    }

    ...
}
```

The same principles are analogous to `@ComputedRelationship`s.

Lifecycle Hooks
---------------

Lifecycle hooks allow custom business logic (defined in functions) to be invoked during CRUD operations at four
distinct phases of the client request:

1. *Pre Security* - Executed immediate prior to Elide security check evaluation.
2. *Pre Flush* - Executed immediate prior to Elide flushing the transaction. This is when database calls are first sent
   to the database.
3. *Pre Commit* - Executed immediately prior to transaction commit but after all security checks have been evaluated.
4. *Post Commit* - Executed immediately after transaction commit.

There are two mechanisms to enable lifecycle hooks on a particular model:

1. The simplest mechanism is to [decorate](#annotation-based-hooks) the Elide model or model fields with the life cycle
   hook function class and the conditions of when to invoke it.
2. Lifecycle hook functions can also be [registered](#registered-function-hooks) with the `EntityDictionary` when
   initializing Elide.

Life cycle hooks are simply functions that conform to the following interface:

```java
/**
 * Function which will be invoked for Elide lifecycle triggers
 *
 * @param <T> The elide entity type associated with this callback.
 */
@FunctionalInterface
public interface LifeCycleHook<T> {

    /**
     * Run for a lifecycle event.
     *
     * @param operation CREATE, UPDATE, or DELETE
     * @param phase PRESECURITY, PREFLUSH, PRECOMMIT or POSTCOMMIT
     * @param elideEntity The entity that triggered the event
     * @param requestScope The request scope
     * @param changes Optionally, the changes that were made to the entity
     */
    void execute(
            LifeCycleHookBinding.Operation operation,
            LifeCycleHookBinding.TransactionPhase phase,
            T elideEntity,
            RequestScope requestScope,
            Optional<ChangeSpec> changes
    );
}
```

### Annotation Based Hooks

Model fields can be decorated with a `LifeCycleHookBinding` annotation. The annotation provides the following
information:

1. The hook function to invoke.
2. The model operation (CREATE, UPDATE, or DELETE) that triggers the hook.
3. The transaction phase of when to trigger the hook (PRESECURITY, PREFLUSH, PRECOMMIT, or POSTCOMMIT).
4. For class level triggers, whether or not the hook should be called for each impacted field or exactly once for
   the class.

```java
class Publisher {

    @Id
    private long id;

    @OneToMany(mappedBy = "publisher")
    @LifeCycleHookBinding(operation = UPDATE, phase = PRECOMMIT, hook = PublisherUpdateHook.class)
    private Set<Book> books;
}
```

### Registered Function Hooks

Lifecycle hooks can be registered in Elide directly without an explicit annotation:

```java
// Register a lifecycle hook for deletes on the model Book. Call exactly once.
dictionary.bindTrigger(Book.class, DELETE, PRESECURITY, hook, false);

// Register a lifecycle hook for updates on the Book model's title attribute
dictionary.bindTrigger(Book.class, "title", UPDATE, POSTCOMMIT, hook);

// Register a lifecycle hook for updates on _any_ of the Book model's attributes
dictionary.bindTrigger(Book.class, UPDATE, POSTCOMMIT, hook, true);
```

Dependency Injection
--------------------

Elide does not depend on a specific dependency injection framework. However, Elide can inject entity models, security
checks, lifecycle hooks, and serdes during their construction. Elide provides a framework agnostic, functional
interface to inject entity models:

```java
/**
 * Abstraction around dependency injection.
 */
@FunctionalInterface
public interface Injector {

    /**
     * Inject an Elide object.
     *
     * @param entity object to inject
     */
    void inject(Object entity);

    /**
     * Instantiates a new instance of a class using the DI framework.
     *
     * @param cls The class to instantiate.
     * @return An instance of the class.
     */
    default <T> T instantiate(Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
```

An implementation of this interface can be passed to the `EntityDictionary` during its construction:

```java
EntityDictionary dictionary = new EntityDictionary(
        PermissionExpressions.getExpressions(),
        (obj) -> injector.inject(obj)
);
```

If we're using the `elide-spring-boot*` artifacts, dependency injection is already setup using Spring. If we're using
the [elide-standalone](elide-standalone) artifact, dependency injection is already setup using Jetty's `ServiceLocator`

Validation
----------

Data models can be validated using [bean validation](http://beanvalidation.org/1.0/spec/). This requires *JSR303*
data model annotations and wiring in a bean validator in the `DataStore`.

Type Coercion
-------------

Type coercion between the API and underlying data model has common support across JSON-API and GraphQL and is
covered [here](clientapis#type-coercion).

Inheritance
-----------

Elide supports two kinds of inheritance:

1. Non-entity inheritance via the JPA annotation `@MappedSuperclass`.
2. Entity inheritance via the JPA annotation `@Inheritance`.

Entity inheritance has a few caveats:

1. Only the `InheritanceType.JOINED` and `InheritanceType.SINGLE_TABLE` strategies are supported.
2. Entity relationships whose type is a superclass have different behavior in JSON-API and GraphQL:

    1. JSON-API will return the type and attributes of the subclass (as well as the super class).
    2. GraphQL will return the type and attributes of the superclass only.

API Versions
------------

Elide models can be bound to a specific API version. Once bound, the models will only be visible to API requests
that ask for the specific version. API versions are bound by creating a package-info.java file with the
`ApiVersion` annotation:

```java
@ApiVersion(version = "1.0")
package example.models;

import com.paiondata.elide.annotation.ApiVersion;
```

API versioning is optional configuration. By default, all models have no implicit version. The API client is also
not required to provide a version in its request.By adding a version to one or more packages however, the versioned
models will only be visible when the client provides the corresponding version in its request.

There is an important caveat when using API versioning with JPA models.JPA does not allow two `Entity` classes to
share the same name - even if they belong to different packages. To work around this, we can either:

- Rename the class (class BookV2) but preserve the Elide model (`@Include(type = "book")`) and database table
  (`@Table(name = "book")`) names.
- Rename the entity name (`@Entity(name = "BookV2")`) but preserve the Elide model (`@Include(type = "book")`) and
  class (class Book) names.

Details of how to construct client queries for a specific version can be found [here](clientapis#api-versioning).

Philosophy
----------

<!-- markdown-link-check-disable -->
Data models are intended to be a _view_ on top of the [data store](datastores) or the set of data stores which support
our Elide-based service. While other JPA-based workflows often encourage writing data models that exactly match the
underlying schema of the data store, we propose a strategy of isolation on per-service basis. Namely, we recommend
creating a data model that only supports precisely the bits of data we need from our underlying schema. Often times
there will be no distinction when first building our systems. However, as our systems scale and we develop multiple
services with overlapping data store requirements, isolation often serves as an effective tool to **reduce
interdependency** among services and **maximize the separation of concern**. Overall, while models can correspond to
our underlying data store schema as a one-to-one representation, it's not always strictly necessary and sometimes even
undesirable.
<!-- markdown-link-check-enable -->

As an example, let's consider a situation where we have two Elide-based microservices: one for our application backend
and another for authentication (suppose account creation is performed out-of-band for this example). Assuming both of
these rely on a common data store, they'll both likely want to recognize the same underlying _User_ table. However,
it's quite likely that the authentication service will only ever require information about user **credentials** and the
application service will likely only ever need user **metadata**. More concretely, you could have a system that looks
like the following:

Table schema:

```sql
id
userName
password
firstName
lastName
```

Authentication schema:

```sql
id
userName
password
```

Application schema:

```sql
id
userName
firstName
lastName
```

While we could certainly just use the raw table schema directly (represented as a JPA-annotated data model) and
reuse it across services, the point is that we may be over-exposing information in areas where we may not want to.
In the case of the _User_ object, it's quite apparent that the application service should never be _capable_ of
accidentally exposing a user's private credentials. By creating isolated views per-service on top of common data
stores, we sacrifice a small bit of [DRY principles](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) for much
better isolation and a more targeted service. Likewise, if the underlying table schema is updated with a new field
that neither one of these services needs, neither service requires a rebuild and redeploy since the change is
irrelevant to their function.

**A note about microservices:** Another common technique to building microservices is for each service to have its
own set of data stores entirely independent from other services (i.e. no shared overlap); these data stores are then
synced by other services as necessary through a messaging bus. If our system architecture calls for such a model,
it's quite likely we will follow the same pattern we have outlined here with _one key difference_: the underlying
table schema for our _individual service's data store_ will likely be exactly the same as our service's model
representing it. However, overall, the net effect is the same since only the relevant information delivered over the
bus is stored in our service's schema. In fact, this model is arguably more robust in the sense that if one data
store fails not all services necessarily fail.

Miscellaneous
-------------

### Supporting Cascading Delete on Data Models

Cascading delete is handled through JPA annotation which offers possibility to cascade operations (merge, persist,
refresh, remove) to associated entities. Logic is in JPA and does not utilize database cascades. We can use

```java
@OneToMany(cascade=CascadeType.REMOVE)
```

For example:

```java
@Entity
@Table(name = "author")
@Include(rootLevel = true, name = "author", description = "author data", friendlyName = "author")
public class Author {

    ...

    @OneToMany(cascade = CascadeType.REMOVE)
    public List<Book> books;
}
```

Deleting an author will also delet all of his/her published books in database.
