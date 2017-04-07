# GraphQL Through Elide

Elide now supports a [GraphQL](http://graphql.org/) interface out-of-box to use alongside or instead of [JSON:API](http://jsonapi.org/). To enable this functionality, simply mount the provided endpoint to a specified path.

# Usage

This section will describe various use cases and examples on how to interact with our GraphQL endpoint.

## Making Calls

All calls must be `POST` requests made to the root endpoint. This specific endpoint will depend on where you mount the provided servlet, however, let's assume we have it mounted at `/graphql` for sake of argument. In this case, all requests should be sent as:

```
POST https://yourdomain.com/graphql
```

## Payload Structure

The structure for our GraphQL payloads is generated and, therefore, uniform across all data types. To understand our format, it is strongly recommended that you first have a [basic understanding of GraphQL](http://graphql.org/learn/). The general structure is as follows:

```
rootObjectTypeName([op: FETCH|ADD|REPLACE|DELETE, id: ID-VALUE, data: DATA-OBJ]) {
  exposedFields
}
```

where `rootObjectTypeName` is the name of an [Elide rootable](http://elide.io/pages/guide/01-start.html) object. Any object then can take any of 3 optional arguments.

  * `op` (abbreviated for `operation`) is one of `FETCH`, `REPLACE`, `ADD`, or `DELETE`. The default value is `FETCH` when unspecified.
  * `id` is the identifier for an object. If this value is unspecified the system assumes to be working on a collection of these objects. Otherwise, it is working on the specified id.
  * `data` is the data input object that is used as the argument to the specified `op` argument. This argument is unused in the case of `FETCH` and `DELETE`.

Finally, the `exposedFields` is a specified list of values for the object(s) that the caller expects to be returned.

# Examples
Below are several examples. For each, assume a simple model of `Book` and `Author`. Particularly, the schema for each can be considered:

```java
@Include(rootLevel = true)
@Entity
public class Book {
    @Id public long id;
    public String title;
    public Set<Author> authors;
}
```

```java
@Include(rootLevel = false)
@Entity
public class Author {
    @Id public long id;
    public String name;
    public Set<Book> books;
}
```

## Fetches

A set of `FETCH` examples.

### Collection of Books with ID, Title, and Authors
```
book {
  id,
  title,
  authors
}
```

### Single Book with Title and Authors
```
book(id: 1) {
  title,
  authors
}
```

This request would return a single book with `id: 1` along with its title and all of its authors.

## Replaces

A set of `REPLACE` examples.

### Replace All Book Authors
```
mutation book(op: REPLACE, id: 1, data: {authors:[{ name: "The New Author" }]}) {
  id,
  authors
}
```

## Adds

A set of `ADD` examples.

### Create and Add a New Author to a Book
```
mutation book(op: ADD, id: 1, data: {authors: [{name: "The added author"}]}) {
  id,
  authors
}
```

## Deletes

A set of `DELETE` examples.

### Delete a Book
```
mutation book(op: DELETE, id: 1) {
  id
}
```