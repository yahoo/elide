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
rootObjectTypeName([op: UPSERT|REPLACE|REMOVE|DELETE, ids: [ID-VALUES], data: DATA-OBJ]) {
  exposedFields,
  objectName([op: UPSERT|REPLACE|REMOVE|DELETE, ids: [ID-VALUES], data: DATA-OBJ])
}
```

where `rootObjectTypeName` is the name of an [Elide rootable](http://elide.io/pages/guide/01-start.html) object. Any object then can take any of 3 optional arguments.

  * `op` (abbreviated for `operation`) is one of `UPSERT`, `REPLACE`, `REMOVE` or `DELETE`. The default value is `UPSERT` when unspecified.

     * `UPSERT` - inserts new or edits existing members of a relationship. It is the primary mechanism by which any updates will be made to the model.
     * `REPLACE` - can be thought as a combination of `UPSERT` and `REMOVE`, wherein, any objects not specified in the data argument (that are visible to the user) will be removed.
     * `REMOVE` - only disassociates a relationship. If the underlying store automatically deletes orphans, it will still be deleted.
     * `DELETE` - both disassociates the relationship and it deletes the entity from the persistence store.

  
  * `ids` are used as the ientifier for object(s). This field can hold a singleton value (for a single object) or a list of values to specify multiple ids. If this value is unspecified the system assumes to be working on a collection of these objects. Otherwise, it is working on the specified id.
  *  `data` is the data input object that is used as the argument to the specified `op` argument. This argument is unsupported in the case of `REMOVE`, `DELETE` or while _fetching_ an existing object.

Finally, the `exposedFields` is a specified list of values for the object(s) that the caller expects to be returned.

# Examples
Below are several examples. For each, assume a simple model of `Book`, `Author` and `Publisher`. Particularly, the schema for each can be considered:

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
    public Set<Publisher> publishers;
}
```
```java
@Include(rootLevel = false)
@Entity
public class Publisher {
    @Id public long id;
    public String name;
    public Set<Book> books;
}
```

## UPSERT
A set of `UPSERT` examples.
### Fetch
#### Collection of Books with ID, Title, and Authors
```
book {
  id,
  title,
  authors
}
```

#### Single Book with Title and Authors
```
book(ids: 1) {
  title,
  authors
}
```

This request would return a single book with `id: 1` along with its title and all of its authors.

### Add
#### Create and Add a New Author to a Book
```
mutation book(op: UPSERT, ids: 1, data: {authors: [{name: "The added author"}]}) {
  id,
  authors
}
```

## DELETE

A set of `DELETE` examples. 

### Delete a Book
```
mutation book(op: DELETE, ids: 1) {
  id
}
```
Deletes the book with `id = 1` and removes disassociates all relationships other entities might have with this object. 

## REMOVE 
```
book(ids: 1) {
    authors(op: REMOVE, ids: 3)
}
```
Removes the _association_ between book with `id = 1` and author with `id = 3`, however, the author is still present in the persistence store.
## REPLACE
A set of `REPLACE` examples.

### Replace All Book Authors
```
mutation book(op: REPLACE, ids: 1, data: {authors:[{ name: "The New Author" }]}) {
  id,
  authors
}
```

## Complex Queries
### Replacing a particular nested field
Let's assume that in a complex scenario, we want to update the name of the 18th author of the 9th book. The corresponding query would be,
```
book(ids: 9) {
    id,
    authors(op: REPLACE, ids: 18, data: {name: "New author"}) {
        title
    }
}
```
The above payload structure helps us manipulate a specific entity amongst several different entities linked with to same parent as under.
```
book(id = 9)
| \ \
.. .. authors(id = 18)
      |
      name
```
### Replacing two seperate fields linked to the same parent
Let's say we want to replace the title of two seperate books associated with the same author. The corresponding query would look like,
```
author(ids: 1) {
    id, 
    books(op: REPLACE, data: [{id: 1, title: "New title"}, {id: 2, title: "New title"}]) {
        id
    }
}
```
The above payload structure helps us manipulate attributes associated with two different entities having the same parent entity as under.
```
author
|     \
|      \
books   books
|  \    |  \
|   \  ...  title
...  title
```
### Replacing fields of two seperate entities associated to the same parent
Now lets say we want to modify a ``Book`` and a `Publisher` name. This can be accomplished in a single query as under.
```
author(ids: 1) {
    id, 
    books(op: REPLACE, data: [{id: 1, title: "New title"}]) {
        id
    }, 
    publishers(op: REPLACE, data: [{id: 1, name: "New name"}]) {
        id
    }
}
```
The above payload structure helps us manipulate attributes of two seperate entities associated with the same parent in a single transaction as under. 
```
author
|     \ 
books  publishers
|      |
title  name
```
### Allowing multiple operations in a single transaction
We can get fancy and allow for multiple operations, like replacing title of a book and deleting a publisher, all in a single transaction. 
```
author(ids: 1) {
    id, 
    books(op: REPLACE, data: [{id: 1, title: "New title"}]) {
        id
    }, 
    publishers(op: REMOVE, id: 1) {
        id
    }
}
```
# Semantics

Below is a chart of expected behavior from GraphQL queries:

| Operation | Id Parameter | Id in Body | Behavior |
| --------- | ------------ | ---------- | -------- |
| Upsert    | True         | True       |  Must match. Implies update. |
|           | True         | False      | Update on persisted object with specified id |
|           | False        | True       | Create new object (referenced by id in body within request) |
|           | False        | False      | Create new object |
| Replace   | True         | True       | Overwrite all specified values in body (including id) |
|           | True         | False      | Overwrite all specified values (no id in body to overwrite) |
|           | False        | True       | Remove from collection except if id exists, it won't be created |
|           | False        | False      | Totally new collection values |
| Remove    | True         | True       | Removes the association with parent root object. |
|           | True         | False      | Boom. |
|           | False        | True       | Boom. |
|           | False        | False      | Boom. |
| Delete    | True         | True       | Must match. Delete single. |
|           | True         | False      | Must match. Delete single. |
|           | False        | True       | Delete matching id's |
|           | False        | False      | Boom. |

**NOTE:** If the id parameter is specified, it is always used as a **lookup** key for an already persisted object. Additionally, if the id parameter is specified outside of the data body, then the data must be a _single_ element list containing the proper object.