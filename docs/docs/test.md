---
sidebar_position: 11
title: Test
---

The [elide-test-helpers](https://github.com/paion-data/elide/tree/master/elide-test) package provides a JSON-API and
GraphQL type safe DSL that simplifies adding integration tests to your service.  The DSLs are designed to work with
[Rest Assured](http://rest-assured.io/).

Dependencies
------------

The tests described here are based on a [the getting started example repo][elide-demo].

The example leverages:

1. [Elide Spring Boot Starter][elide-spring] for running the test service and setting up Elide.
2. [JUnit 5](https://junit.org/junit5/) for adding tests.
3. [elide-test-helpers](https://github.com/paion-data/elide/tree/master/elide-test) for the JSON-API and GraphQL DSLs.
4. [Rest Assured](http://rest-assured.io/) for issuing HTTP requests against the test service.
5. [Spring Boot Test Starter](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-test) for
   adding test data for each test.
6. [H2 In Memory Database](https://www.h2database.com/html/main.html) for an in memory test database.

### Maven

```xml
<dependency>
    <groupId>com.paiondata.elide</groupId>
    <artifactId>elide-spring-boot-starter</artifactId>
    <version>${elide.version}</version>
</dependency>

<dependency>
    <groupId>com.paiondata.elide</groupId>
    <artifactId>elide-test-helpers</artifactId>
    <version>${elide.version}</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>com.jayway.restassured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>2.9.0</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <version>${spring.version}</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.5.2</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>5.5.2</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>1.4.197</version>
</dependency>
```

Setup
-----

Using elide with Spring Boot, we can set up a test service for integration tests by having our test classes extend a
common test base class like this one:

```java
/**
 * Base class for running a set of functional Elide tests.  This class
 * sets up an Elide instance with an in-memory H2 database.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTest {

    @LocalServerPort
    int port;

    @BeforeAll
    public void setUp() {
        RestAssured.port = port;
    }
}
```

JSON-API DSL
------------

Using Rest Assured and the JSON-API DSL, we can issue JSON-API requests and verify responses against our test service.
This example uses Spring Boot to initialize the H2 database with a clean set of test records.

```java
    @Test
    @Sql(statements = {
            "DELETE FROM ArtifactVersion; DELETE FROM ArtifactProduct; DELETE FROM ArtifactGroup;",
            "INSERT INTO ArtifactGroup (name, commonName, description) VALUES\n" +
                    "\t\t('com.example.repository','Example Repository','The code for this project');"
    })
    void jsonApiGetTest() {
        when()
            .get("/api/v1/group")
            .then()
            .log().all()
            .body(equalTo(
                data(
                    resource(
                        type( "group"),
                        id("com.example.repository"),
                        attributes(
                            attr("commonName", "Example Repository"),
                            attr("description", "The code for this project")
                        ),
                        relationships(
                            relation("products")
                        )
                    )
                ).toJSON())
            )
            .log().all()
            .statusCode(HttpStatus.SC_OK);
    }
```

The complete set of static DSL operators for JSON-API can be found
[here](https://github.com/paion-data/elide/blob/master/elide-test/src/main/java/com/paiondata/elide/test/jsonapi/JsonApiDSL.java).

GraphQL DSL
-----------

Using Rest Assured and the GraphQL DSL, we can issue GraphQL requests and verify responses against our test service
like this:

```java
    @Test
    @Sql(statements = {
            "DELETE FROM ArtifactVersion; DELETE FROM ArtifactProduct; DELETE FROM ArtifactGroup;",
            "INSERT INTO ArtifactGroup (name, commonName, description) VALUES\n" +
                    "\t\t('com.example.repository','Example Repository','The code for this project');",
            "INSERT INTO ArtifactGroup (name, commonName, description) VALUES\n" +
                    "\t\t('com.paiondata.elide','Elide','The magical library powering this project');"
    })
    void graphqlTest() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body("{ \"query\" : \"" + GraphQLDSL.document(
                query(
                    selection(
                        field("group",
                            selections(
                                field("name"),
                                field("commonName"),
                                field("description")
                            )
                        )
                    )
                )
            ).toQuery() + "\" }"
        )
        .when()
            .post("/graphql/api/v1")
            .then()
            .body(equalTo(GraphQLDSL.document(
                selection(
                    field(
                        "group",
                        selections(
                            field("name", "com.example.repository"),
                            field( "commonName", "Example Repository"),
                            field("description", "The code for this project")
                        ),
                        selections(
                            field("name", "com.paiondata.elide"),
                            field( "commonName", "Elide"),
                            field("description", "The magical library powering this project")
                        )
                    )
                )
            ).toResponse()))
            .statusCode(HttpStatus.SC_OK);
    }
```

The complete set of static DSL operators for GraphQL can be found [here](https://github.com/paion-data/elide/blob/master/elide-test/src/main/java/com/paiondata/elide/test/graphql/GraphQLDSL.java).

[elide-demo]: https://github.com/paion-data/elide-spring-boot-example
[elide-spring]: https://github.com/paion-data/elide/tree/master/elide-spring/elide-spring-boot-starter
