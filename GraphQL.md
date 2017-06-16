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
    attributeName1,
    attributeName2,
    ...
    attributeNameN,
    relationshipName1([op: UPSERT|REPLACE|REMOVE|DELETE, ids: [ID-VALUES], data: DATA-OBJ]),
    relationshipName2([op: UPSERT|REPLACE|REMOVE|DELETE, ids: [ID-VALUES], data: DATA-OBJ]),
    ...
    relationshipNameN([op: UPSERT|REPLACE|REMOVE|DELETE, ids: [ID-VALUES], data: DATA-OBJ])
}
```

where `rootObjectTypeName` is the name of an [Elide rootable](http://elide.io/pages/guide/01-start.html) object. Any object then can take any of the 4 optional arguments.

  * `op` (abbreviated for `operation`) is one of `UPSERT`, `REPLACE`, `REMOVE` or `DELETE`. The default value is `FETCH` when unspecified.

     * `UPSERT` - inserts new or edits existing members of a relationship. It is the primary mechanism by which any updates will be made to the model.
     * `REPLACE` - can be thought as a combination of `UPSERT` and `REMOVE`, wherein, any objects not specified in the data argument (that are visible to the user) will be removed.
     * `REMOVE` - only disassociates a relationship. If the underlying store automatically deletes orphans, it will still be deleted.
     * `DELETE` - both disassociates the relationship and it deletes the entity from the persistence store.
     * `FETCH` - fetches an already existing entity from the persistent datastore. 
  
  * `ids` are used as the identifier for object(s). This field holds a list of ids. If this value is unspecified the system assumes to be working on a collection of these objects. Otherwise, it is working on the specified ids. This argument is unsupported in the case of `UPSERT` and `REPLACE`.
  *  `data` is the data input object that is used as the argument to the specified `op` argument. This argument is unsupported in the case of `REMOVE`, `DELETE` or while _fetching_ an existing object.

Finally, the `attributeName` is a specified list of values for the object(s) that the caller expects to be returned. 

# Examples
Below are several examples. For each, assume a simple model of `Book`, `Author` and `Publisher`. Particularly, the schema for each can be considered:

```java
@Entity
@Table(name = "book")
@Include(rootLevel = true)
public class Book {
    @Id public long id;
    public String title;
    @ManyToMany
    public Set<Author> authors;
    @ManyToOne
    Publisher publisher;
}
```
```java
@Entity
@Table(name = "author")
@Include(rootLevel = false)
public class Author {
    @Id public long id;
    public String name;
    @ManyToMany
    public Set<Book> books;
}
```
```java
@Entity
@Table(name = "publisher")
@Include(rootLevel = false)
public class Publisher {
    @Id public long id;
    public String name;
    @OneToMany
    public Set<Book> books;
}
```

## FETCH
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
book(ids: [1]) {
  title,
  authors
}
```

This request would return a single book with `id: 1` along with its title and all of its authors.

## UPSERT
A set of `UPSERT` examples.
#### Create and Add a New Author to a Book
```
mutation book(op: UPSERT, data: {authors: [{name: "The added author"}]}) {
  id,
  authors
}
```

## DELETE

A set of `DELETE` examples. 

### Delete a Book
```
mutation book(op: DELETE, ids: [1]) {
  id
}
```
Deletes the book with `id = 1` and removes disassociates all relationships other entities might have with this object. 

## REMOVE 
```
book(ids: [1]) {
    authors(op: REMOVE, ids: [3])
}
```
Removes the _association_ between book with `id = 1` and author with `id = 3`, however, the author is still present in the persistence store.
## REPLACE
A set of `REPLACE` examples.

### Replace All Book Authors
```
mutation book(op: REPLACE, data: {authors:[{ name: "The New Author" }]}) {
  id,
  authors
}
```

## Complex Queries
### Replacing a particular nested field
Let's assume that in a complex scenario, we want to update the name of the 18th author of the 9th book. The corresponding query would be,
```
book(ids: [9]) {
    id,
    authors(op: REPLACE, data: {name: "New author"}) {
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
author(ids: [1]) {
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
books(ids: [1]) {
    id, 
    authors(op: REPLACE, data: [{id: 1, name: "New author"}]) {
        id
    }, 
    publisher(op: REPLACE, data: [{id: 1, name: "New name"}]) {
        id
    }
}
```
The above payload structure helps us manipulate attributes of two seperate entities associated with the same parent in a single transaction as under. 
```
books
|      \ 
authors  publisher
|      |
name  name
```
### Allowing multiple operations in a single transaction
We can get fancy and allow for multiple operations, like replacing title of a book and deleting a publisher, all in a single transaction. 
```
books(ids: [1]) {
    id, 
    authors(op: REPLACE, data: [{id: 1, name: "New author"}]) {
        id
    }, 
    publisher(op: REMOVE, ids: [1]) {
        id
    }
}
```
# Semantics
While writing custom queries, you must take care of operations which do and do not allow the `data` and `ids` parameter fields as under.

| Operation | Data | Ids |
| --------- |------|-----| 
| Upsert    | ✓    | X   | 
| Fetch     | X    | ✓   |
| Replace   | ✓    | X   | 
| Remove    | X    | ✓   | 
| Delete    | X    | ✓   |

**NOTE:**
* Creating objects with _UPSERT_ behave much like sql UPSERT, wherein, we first attempt to load the object, and if not present, we create it. 
* If the id parameter is specified, it is always used as a **lookup** key for an already persisted object. Additionally, if the id parameter is specified outside of the data body, then the data must be a _single_ element list containing the proper object. 
## graphQL Map Type 
Elide provides [map input types for graphQL queries](https://github.com/yahoo/elide/blob/add-data-fetcher/elide-core/src/main/java/com/yahoo/elide/graphql/GraphQLConversionUtils.java#L132). GraphQL inherently doesn't support map input types. We mimic
maps by creating a list of key/value pairs from parent and attribute classes and supplying it to native `GraphQLList` object type. 