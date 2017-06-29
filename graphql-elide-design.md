# GraphQL Support for Elide

## Overview

The next development iteration for Elide focuses on seamless conversion from REST to a graphQL endpoint by simply mounting the provided endpoint to a specified path. Using GraphQL can be a more relevant approach in cases when, say, the user API is not using REST specific services like HATEOAS. Among others, it provides the following key advantages - 
- Single endpoint to access all the data
- No need to tailor specific endpoints for each view/collection
- Flexibility to access only the data the client needs in a single request

Elide utilizes [graphql-java's](http://graphql-java.readthedocs.io/en/latest/index.html) execution strategies to execute a graphql  Query/Mutation against an elide Schema. 

## Structure

The main files are located under the package `graphql` within `elide-core`
```
+-- elide-core
.
.
.
+-- graphql
   +-- operations
|       -- UpdateOperation.java
   +-- sort
|       -- Sort.java
-- Environment.java
-- GraphQLConversionUtils.java
-- GraphQLEndpoint.java
-- ModelBuilder.java
-- MutableGraphQLInputObjectType.java
-- NonEntityDictionary.java
-- PersistentResourceFetcher.java
-- RelationshipOp.java
```
The tests are located under the package `graphql` within `elide-core` > `test` 

## Classes

### Operation class
First let's go over the supported operations for graphql queries/mutations. These are defined under `RelationshipOp.java` as below - 
```
public enum RelationshipOp {
    FETCH,
    DELETE,
    ADD,
    REPLACE;
}
```
Refer to the `GraphQL.md` for a thorough explanation of the functionality of each operation. 

### Dictionary class
Maps non-elide entities to/from entity type names by overriding `bindEntity` method which adds the given Class `cls` to the entity dictionary. Used in `GraphQLConversionUtils.java` to build the non-entity dictionary.
```
/**
 * A set of reflection utilities for non-Elide entities.
 */
public class NonEntityDictionary extends EntityDictionary {
     @Override
    public void bindEntity(Class<?> cls) {
        ...
    }
}
```

### GraphQLEndpoint
Uses `@Path` JAX-RS annotation to specify the graphql path to mount over an existing endpoint. 

```
@Path("/graphQLx")
public class GraphQLEndpoint {
    ...
    
    [QUESTION: Why do we have a POST route defined here? 
    It's being used in the GraphQLTest.java with regular graphql mutations, 
    isn't that what UPSERT supposed to do?]
}
```

### GraphQLConversionUtils 
Contains a bunch of utility methods to convert a class to a graphql query or input type. This is particularly nice since the methods defined here take in a generic `Class<T>` parameter, making this class Elide independent, hence usable in independent `graphql-java` projects. For example, 
```
/**
    classToScalarType method can be used to convert java primitive types to a GraphQLType
*/
public GraphQLScalarType classToScalarType(Class<?> clazz) {
    if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
        return Scalars.GraphQLInt;
    }
    ...
}
etc.
```
These methods can come in really handy while building a graphql model from POJOs. (eg: see `ModelBuilder.java`)

### MutableGraphQLInputObjectType 
Very similar to the `graphql-java` class `GraphQLInputObjectType.java` except fields can be added after input object has been created giving it a mutable behaviour. 

### Model builder class
`ModelBuilder.java` is a useful class since it builds a graphQL schema from an elide dictionary. The major things happening here are as under - 
- define all the permitted query arguments along with their valid graphQL data types in the class constructor. 
- Traverse the object graph and construct the GraphQL input object types in the `build()` method.
- Create the root object and graphQL output object types along with  defining each field that can be queried or mutated in the `buildQueryObject()` method.
- build the schema!

### Environment 

Think of this class as the serving _context_ required to execute graphql mutations/queries, encapsulating the graphQL request environment. It extracts all the arguments passed into the user graphql query. The following are the fields along with their types, useful while, say, unwrapping an optional value. 

| Arguments    |         Type              | Description                                                    |
| ---------    | -------------------       |-------------                                                   |
| requestScope | `RequestScope`            |referse to the object request scope                             |
|    id        |`List<Optional<String>>`   |holds the `ids` field passed in the query/mutation              |
|  source      |      `Object`             |refers to the source object for the `DataFetchingEnvironment`   |
|parentResource|`PersistentResource`       |refers to the parent of an object, say, within a nested query   |
|parentType    |`GraphQLType`              |graphql type of the parent                                      |
| outputType   | `GraphQLType`             |object output type                                              |
|field         | `field`                   |contains all the fields requested as a `List` called `arguments`|
|sort          |`Optional<String> `        |holds the 'sort' field passed in the query/mutation             |
| data         |`List<Map<String, Object>>`|holds the 'data' field passed in the query/mutation             |
|filters       |`Optional<String>`         |holds the 'filter' field passed in the query/mutation           |
| offset       |`Optional<String>`         |holds the 'offset' field passed in the query/mutation           |
| first        |`Optional<String>`         |holds the 'first' field passed in the query/mutation            |

### PersistentResourceFetcher
This is where the magic happens to fetch the data for the custom operations that elide provides. This is done by overriding the `get` method provided by the `DataFetcher` interface by `graphql-java`, wherein we delegate the control to the appropriate method based on the value of the `op` expression or default it to `FETCH` if one isn't provided. 
#### methods - 
- **fetching an object -** 
```
/**
    fetches an object from internal datastore 
*/
fetchObject(environment) {
    /**
        perform sanity checks like fetch called with data field is invalid, 
        filtering with id field is invalid,
        sorting with id field is invalid
    */
    if (!request.data.isEmpty()) {
        throw new BadRequestException("FETCH must not include data.");
    }
    if ((request.id != null && !request.id.isEmpty())) {
        if (request.filters.isPresent()) {
            throw new WebApplicationException("You may not filter when loading by id");
        }
        if(request.sort.isPresent()) {
            throw new WebApplicationException("You may not sort when loading by id");
        }
    }
    ...
    /**based of the outputType in environment, fetch the data and return
    the loadRecords() method in persistentResource would retrieve all the entries
    based on the loadClass
    the loadRecord() method in persistentResource takes in an id parameter and 
    returns a single entity from the DB
    */
    
    
    /**
        Once we fetch the records, check the sorting/pagination/filtering arguments 
        and process the list **in-memory** accordingly in the **same** order
    */
}
```
To handle sorting, a new class is defined called `Sort` under the package `sort`. The constructor takes in a `String` corresponding to the value to the sorting argument provided by the user. It has the following methods - 
```
/**
    parse the sorting argument to retrieve order: ASC/DESC
*/
private String parseSortRule() {
    char firstCharacter = sortArg.charAt(0);
    if (firstCharacter == '-') {
        ...
    }
    if (firstCharacter == '+') {
        ...
    }
}

/**
 * This method sorts the list "records" in-memory, 
 * NOTE: we are not delegating this to the SQL orderBy clause to sort on disk
 */
public void sort(List<PersistentResource> records, RequestScope requestScope) {
    ...
}
```

To handle in-memory pagination, a new method is defined in `PersistenceResourceFetcher.java`. 
```
/**
 * paginate list in-memory
 * @param records A list of records to paginate
 * @param request Environment instance containing pagination parameters
 * @return paginated list
 */
private static Object paginate(List<PersistentResource> records, Environment request) {
    int first, offset, start, end;
    try{
        first = Integer.parseInt(request.first.get());
    } catch (NumberFormatException e) {
        throw new NumberFormatException("Please enter a valid integer as an argument to 'first'");
    }
    if(!request.offset.isPresent()) {
        start = 0;
        end = first;
    } else {
        try {
            offset = Integer.parseInt(request.offset.get());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Please enter a valid integer as an argument to 'offset'");
        }
        start = first;
        end = offset + first;
    }
    return records.subList(start, end);
}
```
One thing to note here is that the `offset` and `first` argument are interpreted as under - 
```
…
…
…
Offset …
…
…
…
First …
```
Meaning, offset specifies the number of rows to skip from the 0th row and first specifies the number of rows to return beginning from the offset. Example: offset: 3, first: 9 would return rows 3 to 12. 
[QUESTION: myself and Dan had a little confusion interpreting what the first argument means? We discussed that we could perhaps change “first” to “limit” or “size”. As json-api specs uses “first” as the keyword to refer to first page of data which might lead to some confusion for the user.]

- **adding an object -**
```
/**
    creates a new object from an entity class
*/

Object createObject(Environment request) {
    /* perform sanity checks like id shouldn't be provided while creating object etc. */
    
    /* generate random UUID, this will be changed if newly created object is referenced within a parent object provided in the user query itself */
    uuid = UUID.randomUUID().toString();
    
    if (request.outputType instanceof GraphQLObjectType) {
        /* no parent, object created sits at the root */
        objectType = (GraphQLObjectType) request.outputType;

        PersistentResource createdObject =  PersistentResource.createObject(null, dictionary.getEntityClass(objectType.getName()), request.requestScope, uuid);

        //if user has specified 'first' argument and it's 0, we don't return the newly created object
        if(request.first.isPresent() && request.first.get().equals("0")) return null;

        //else we do
        return createdObject;
    }
    else if (request.outputType instanceof GraphQLList) {
        /* has parent, set uuid to parent's id so they can referenced throughout the document */
        if(request.parentResource != null)
        uuid = String.valueOf(PersistentResource.getValue(request.parentResource.getObject(), "id", request.requestScope));
    
        /* loop through "data" (to fetch object fields) and create the object */
        for (Map<String, Object> input : request.data) {
            Class<?> entityClass = dictionary.getEntityClass(objectType.getName());
            PersistentResource toCreate = PersistentResource.createObject(null, entityClass, request.requestScope, uuid);
            ...
    }
    /**
    Handle pagination and sorting in the response object, a use-case could be when a user creates 1000 new rows but doesn’t want all of it to be returned as a response from the API, and can thus provide a limiting argument with the query. 
    */
    ...
}
```

# TODO:
- this finishes fetch and 1/2 of _UPSERT_ according to the new specs, next step is to support _updating_ an object to finish _UPSERT_.
- moving on, handle _DELETE, REPLACE, REMOVE_.

## Test cases - 
```
+-- elide-core
.
.
.
+-- test
   +-- java
       +-- graphql
|       -- AbstractGraphQLTest.java
|       -- AbstractPersistentResourceFetcherTest.java
|       -- FetcherAddTest.java
|       -- FetcherDeleteTest.java
|       -- FetcherFetchTest.java
|       -- FetcherReplaceTest.java
|       -- GraphQLTest.java
|       -- ModelBuilderTest.java
```

### AbstractPersistentResourceFetcherTest 
Sets up some stub data to test queries on and methods to execute these queries and return true/false by asserting it with expected result. 

### FetcherAddTest
Comprehensive list of tests to test adding a new object to datastore.

Example tests - 
```
/**
 * Test the Add operation.
 */
public class FetcherAddTest extends AbstractPersistentResourceFetcherTest {
    @Test
    public void testCreateRootSingle() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                        "book(op: UPSERT, data: {title: \"Book Numero Dos\"} ) { " +
                        "title " +
                    "} " +
                "}";
        String expectedResponse =
                "{" +
                    "\"book\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                    "}]" +
                "}";
        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testCreateRootCollection() throws JsonProcessingException {
        String graphQLRequest =
                "mutation { " +
                    "book(op: UPSERT, data: [{title: \"Book Numero Dos\"},{title:\"Book Numero Tres\"}] ) { " +
                        "title " +
                    "} " +
                "}";
        String expectedResponse =
                "{" +
                    "\"book\":[{" +
                        "\"title\":\"Book Numero Dos\"" +
                    "},{" +
                        "\"title\":\"Book Numero Tres\"" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }
}
```

### FetcherDeleteTest
Comprehensive list of tests to test deleting an already existing object in datastore.

### FetcherFetchTest
Comprehensive list of tests to test fetching an already existing object in datastore.

Example tests - 
```
/**
 * Test the Fetch operation.
 */
public class FetcherFetchTest extends AbstractPersistentResourceFetcherTest {
    @Test
    public void testRootSingle() throws JsonProcessingException {
        String graphQLRequest =
                "{" +
                    "book(ids: [\"1\"]) { " +
                        "id " +
                        "title " +
                    "}" +
                "}";
        String expectedResponse =
                "{" +
                    "\"book\":[{" +
                        "\"id\":\"1\"," +
                        "\"title\":\"Libro Uno\"" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }

    @Test
    public void testRootMultipleIds() throws JsonProcessingException {
        String graphQLRequest =
                "{ " +
                    "book(ids: [\"1\", \"2\"]) { " +
                        "id " +
                        "title " +
                    "} " +
                "}";
        String expectedResponse =
                "{" +
                    "\"book\":[{" +
                        "\"id\":\"1\"," +
                        "\"title\":\"Libro Uno\"" +
                    "}," +
                    "{" +
                        "\"id\":\"2\"," +
                        "\"title\":\"Libro Dos\"" +
                    "}]" +
                "}";

        assertQueryEquals(graphQLRequest, expectedResponse);
    }
}
```

### FetcherReplaceTest
Comprehensive list of tests to test replacing an already existing object in datastore.


#### TODO - 
- combine add and replace into `FetcherUpsertTest.java`
- add tests to test `remove op`