/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.paiondata.elide.test.graphql.GraphQLDSL.argument;
import static com.paiondata.elide.test.graphql.GraphQLDSL.arguments;
import static com.paiondata.elide.test.graphql.GraphQLDSL.field;
import static com.paiondata.elide.test.graphql.GraphQLDSL.mutation;
import static com.paiondata.elide.test.graphql.GraphQLDSL.query;
import static com.paiondata.elide.test.graphql.GraphQLDSL.selection;
import static com.paiondata.elide.test.graphql.GraphQLDSL.selections;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.atomicOperation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.atomicOperations;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.data;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.lid;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.links;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.patchOperation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.patchSet;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.ref;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relationship;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relationships;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static com.paiondata.elide.test.jsonapi.elements.PatchOperationType.add;
import static com.paiondata.elide.test.jsonapi.elements.PatchOperationType.remove;
import static com.paiondata.elide.test.jsonapi.elements.PatchOperationType.replace;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.graphql.GraphQLDSL;
import com.paiondata.elide.test.jsonapi.elements.AtomicOperationCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import java.util.Map;
/**
 * Example functional test.
 */
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = "classpath:db/test_init.sql",
        statements = "INSERT INTO ArtifactGroup (name, commonName, description, deprecated) VALUES\n"
                + "\t\t('com.example.repository','Example Repository','The code for this project', false);"
)
@TestPropertySource(
        properties = {
                "elide.json-api.links.enabled=true",
                "elide.async.export.enabled=false",
        }
)
@ActiveProfiles("default")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ControllerTest extends IntegrationTest {
    private String baseUrl;

    @BeforeAll
    @Override
    public void setUp() {
        super.setUp();
        baseUrl = "https://elide.io/json/";
    }

    /**
     * This test demonstrates an example test using the JSON-API DSL.
     */
    @Test
    public void jsonApiGetTest() {
        when()
                .get("/json/group")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type("group"),
                                        id("com.example.repository"),
                                        attributes(
                                                attr("commonName", "Example Repository"),
                                                attr("deprecated", false),
                                                attr("description", "The code for this project")
                                        ),
                                        links(
                                                attr("self", baseUrl + "group/com.example.repository")
                                        ),
                                        relationships(
                                                relation(
                                                        "products",
                                                        links(
                                                                attr("self", baseUrl + "group/com.example.repository/relationships/products"),
                                                                attr("related", baseUrl + "group/com.example.repository/products")
                                                        )
                                                )
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void versionedJsonApiGetTest() {
        given()
                .header("ApiVersion", "1.0")
                .when()
                .get("/json/group")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type("group"),
                                        id("com.example.repository"),
                                        attributes(
                                                attr("deprecated", false),
                                                attr("title", "Example Repository")
                                        ),
                                        links(
                                                attr("self", baseUrl + "group/com.example.repository")
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void versionedJsonApiGetPathTest() {
        given()
                .when()
                .get("/json/v1.0/group")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type("group"),
                                        id("com.example.repository"),
                                        attributes(
                                                attr("deprecated", false),
                                                attr("title", "Example Repository")
                                        ),
                                        links(
                                                attr("self", baseUrl + "v1.0/group/com.example.repository")
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void jsonApiPatchTest() {
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        datum(
                                resource(
                                        type("group"),
                                        id("com.example.repository"),
                                        attributes(
                                                attr("commonName", "Changed It.")
                                        )
                                )
                        )
                )
                .when()
                .patch("/json/group/com.example.repository")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);


        when()
                .get("/json/group")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type("group"),
                                        id("com.example.repository"),
                                        attributes(
                                                attr("commonName", "Changed It."),
                                                attr("deprecated", false),
                                                attr("description", "The code for this project")
                                        ),
                                        links(
                                                attr("self", baseUrl + "group/com.example.repository")
                                        ),
                                        relationships(
                                                relation(
                                                        "products",
                                                        links(
                                                                attr("self", baseUrl + "group/com.example.repository/relationships/products"),
                                                                attr("related", baseUrl + "group/com.example.repository/products")
                                                        )

                                                )
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void jsonApiPostLidTest() {
        String personId = "1";
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        datum(
                                resource(
                                        type("person"),
                                        lid("0eeabd1d-70a9-4e9a-8734-9f2d6b43b2ea"),
                                        attributes(
                                                attr("firstName", "John"),
                                                attr("lastName", "Doe")
                                        ),
                                        relationships(
                                                relation("bestFriend",
                                                        true,
                                                        resource(
                                                                type("person"),
                                                                lid("0eeabd1d-70a9-4e9a-8734-9f2d6b43b2ea")))
                                                )
                                )
                        )
                )
                .when()
                .post("/json/person")
                .then()
                .body(equalTo(
                        datum(
                                resource(
                                        type("person"),
                                        id(personId),
                                        attributes(
                                                attr("firstName", "John"),
                                                attr("lastName", "Doe")
                                        ),
                                        links(
                                                attr("self", baseUrl + "person/" + personId)
                                        ),
                                        relationships(
                                                relation(
                                                        "bestFriend",
                                                        true,
                                                        links(
                                                                attr("self", baseUrl + "person/" + personId + "/relationships/bestFriend"),
                                                                attr("related", baseUrl + "person/" + personId + "/bestFriend")
                                                        ),
                                                        resource(type("person"), id(personId))

                                                )
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_CREATED);
    }

    @Test
    public void jsonForbiddenApiPatchTest() {
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        datum(
                                resource(
                                        type("group"),
                                        id("com.example.repository"),
                                        attributes(
                                                attr("commonName", "Changed It."),
                                                attr("deprecated", true)
                                        )
                                )
                        )
                )
                .when()
                .patch("/json/group/com.example.repository")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void jsonApiPatchExtensionTest() {
        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
                .accept(JsonApi.JsonPatch.MEDIA_TYPE)
                .body(
                        patchSet(
                                patchOperation(add, "/group",
                                        resource(
                                                type("group"),
                                                id("com.example.patch1"),
                                                attributes(
                                                        attr("commonName", "Foo")
                                                )
                                        )
                                ),
                                patchOperation(add, "/group",
                                        resource(
                                                type("group"),
                                                id("com.example.patch2"),
                                                attributes(
                                                        attr("commonName", "Foo2")
                                                )
                                        )
                                ),
                                patchOperation(replace, "/group/com.example.patch2",
                                        resource(
                                                type("group"),
                                                id("com.example.patch2"),
                                                attributes(
                                                        attr("description", "Updated Description")
                                                )
                                        )
                                )
                        )
                )
                .when()
                .patch("/json")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        String result = response.asString();
        String expected = """
                [{"data":{"type":"group","id":"com.example.patch1","attributes":{"commonName":"Foo","deprecated":false,"description":""},"relationships":{"products":{"links":{"self":"https://elide.io/json/group/com.example.patch1/relationships/products","related":"https://elide.io/json/group/com.example.patch1/products"},"data":[]}},"links":{"self":"https://elide.io/json/group/com.example.patch1"}}},{"data":{"type":"group","id":"com.example.patch2","attributes":{"commonName":"Foo2","deprecated":false,"description":"Updated Description"},"relationships":{"products":{"links":{"self":"https://elide.io/json/group/com.example.patch2/relationships/products","related":"https://elide.io/json/group/com.example.patch2/products"},"data":[]}},"links":{"self":"https://elide.io/json/group/com.example.patch2"}}},{"data":null}]""";
        assertEquals(expected, result);

        ExtractableResponse<Response> deleteResponse = given()
                .contentType(JsonApi.JsonPatch.MEDIA_TYPE)
                .accept(JsonApi.JsonPatch.MEDIA_TYPE)
                .body(
                        patchSet(
                                patchOperation(remove, "/group",
                                        resource(
                                                type("group"),
                                                id("com.example.patch1")
                                        )
                                ),
                                patchOperation(remove, "/group",
                                        resource(
                                                type("group"),
                                                id("com.example.patch2")
                                        )
                                )
                        )
                )
                .when()
                .patch("/json")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        String deleteResult = deleteResponse.asString();
        String deleteExpected = """
                [{"data":null},{"data":null}]""";
        assertEquals(deleteExpected, deleteResult);
    }

    @Test
    public void jsonApiAtomicOperationsExtensionTest() {
        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.add, "/group",
                                        datum(resource(
                                                type("group"),
                                                id("com.example.operations1"),
                                                attributes(
                                                        attr("commonName", "Foo1")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.add, "/group",
                                        datum(resource(
                                                type("group"),
                                                id("com.example.operations2"),
                                                attributes(
                                                        attr("commonName", "Foo2")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.update, "/group/com.example.operations2",
                                        datum(resource(
                                                type("group"),
                                                id("com.example.operations2"),
                                                attributes(
                                                        attr("description", "Updated Description")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.add, "/group/com.example.operations2/products",
                                        datum(resource(
                                                type("product"),
                                                id("com.example.operations.product1"),
                                                attributes(
                                                        attr("commonName", "Product1")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.update, "/group/com.example.operations2/products/com.example.operations.product1",
                                        datum(resource(
                                                type("product"),
                                                id("com.example.operations.product1"),
                                                attributes(
                                                        attr("description", "Product1 Description")
                                                )
                                        ))
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();

        String result = response.asString();
        String expected = """
                {"atomic:results":[{"data":{"type":"group","id":"com.example.operations1","attributes":{"commonName":"Foo1","deprecated":false,"description":""},"relationships":{"products":{"links":{"self":"https://elide.io/json/group/com.example.operations1/relationships/products","related":"https://elide.io/json/group/com.example.operations1/products"},"data":[]}},"links":{"self":"https://elide.io/json/group/com.example.operations1"}}},{"data":{"type":"group","id":"com.example.operations2","attributes":{"commonName":"Foo2","deprecated":false,"description":"Updated Description"},"relationships":{"products":{"links":{"self":"https://elide.io/json/group/com.example.operations2/relationships/products","related":"https://elide.io/json/group/com.example.operations2/products"},"data":[{"type":"product","id":"com.example.operations.product1"}]}},"links":{"self":"https://elide.io/json/group/com.example.operations2"}}},{"data":null},{"data":{"type":"product","id":"com.example.operations.product1","attributes":{"commonName":"Product1","description":"Product1 Description"},"relationships":{"group":{"links":{"self":"https://elide.io/json/group/com.example.operations2/products/com.example.operations.product1/relationships/group","related":"https://elide.io/json/group/com.example.operations2/products/com.example.operations.product1/group"},"data":{"type":"group","id":"com.example.operations2"}},"maintainers":{"links":{"self":"https://elide.io/json/group/com.example.operations2/products/com.example.operations.product1/relationships/maintainers","related":"https://elide.io/json/group/com.example.operations2/products/com.example.operations.product1/maintainers"},"data":[]},"versions":{"links":{"self":"https://elide.io/json/group/com.example.operations2/products/com.example.operations.product1/relationships/versions","related":"https://elide.io/json/group/com.example.operations2/products/com.example.operations.product1/versions"},"data":[]}},"links":{"self":"https://elide.io/json/group/com.example.operations2/products/com.example.operations.product1"}}},{"data":null}]}""";
        assertEquals(expected, result);

        ExtractableResponse<Response> deleteResponse = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.remove,
                                        ref(
                                                type("group"),
                                                id("com.example.operations1")
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.remove,
                                        ref(
                                                type("group"),
                                                id("com.example.operations2")
                                        )
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        String deleteResult = deleteResponse.asString();
        String deleteExpected = """
                {"atomic:results":[{"data":null},{"data":null}]}""";
        assertEquals(deleteExpected, deleteResult);

    }

    @Test
    public void jsonApiAtomicOperationsExtensionPathInferTest() {
        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.add,
                                        datum(resource(
                                                type("group"),
                                                id("com.example.operationsinfer1"),
                                                attributes(
                                                        attr("commonName", "Foo1")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.add,
                                        datum(resource(
                                                type("group"),
                                                id("com.example.operationsinfer2"),
                                                attributes(
                                                        attr("commonName", "Foo2")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.update,
                                        datum(resource(
                                                type("group"),
                                                id("com.example.operationsinfer2"),
                                                attributes(
                                                        attr("description", "Updated Description")
                                                )
                                        ))
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        String result = response.asString();
        String expected = """
                {"atomic:results":[{"data":{"type":"group","id":"com.example.operationsinfer1","attributes":{"commonName":"Foo1","deprecated":false,"description":""},"relationships":{"products":{"links":{"self":"https://elide.io/json/group/com.example.operationsinfer1/relationships/products","related":"https://elide.io/json/group/com.example.operationsinfer1/products"},"data":[]}},"links":{"self":"https://elide.io/json/group/com.example.operationsinfer1"}}},{"data":{"type":"group","id":"com.example.operationsinfer2","attributes":{"commonName":"Foo2","deprecated":false,"description":"Updated Description"},"relationships":{"products":{"links":{"self":"https://elide.io/json/group/com.example.operationsinfer2/relationships/products","related":"https://elide.io/json/group/com.example.operationsinfer2/products"},"data":[]}},"links":{"self":"https://elide.io/json/group/com.example.operationsinfer2"}}},{"data":null}]}""";
        assertEquals(expected, result);

        ExtractableResponse<Response> deleteResponse = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.remove,
                                        ref(
                                           type("group"),
                                           id("com.example.operationsinfer1")
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.remove,
                                        ref(
                                                type("group"),
                                                id("com.example.operationsinfer2")
                                             )
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        String deleteResult = deleteResponse.asString();
        String deleteExpected = """
                {"atomic:results":[{"data":null},{"data":null}]}""";
        assertEquals(deleteExpected, deleteResult);
    }

    @Test
    public void jsonApiAtomicOperationsExtensionLidTest() {
        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.add,
                                        datum(resource(
                                                type("book"),
                                                lid("f950e6f7-d392-4671-bacd-80966fa3ed5c"),
                                                attributes(
                                                        attr("title", "Book 1")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.add,
                                        datum(resource(
                                                type("book"),
                                                lid("d4fd57f0-3127-4bb6-b4c9-13bc78341737"),
                                                attributes(
                                                        attr("title", "Book 2")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.add,
                                        datum(resource(
                                                type("publisher"),
                                                lid("ed615b7a-b551-4a2d-aa12-56154c1c44aa"),
                                                attributes(
                                                        attr("name", "Publisher")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.update,
                                        ref(type("book"), lid("d4fd57f0-3127-4bb6-b4c9-13bc78341737")),
                                        datum(resource(
                                                type("book"),
                                                lid("d4fd57f0-3127-4bb6-b4c9-13bc78341737"),
                                                attributes(
                                                        attr("title", "Book 2 Updated")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.add,
                                        ref(type("publisher"), lid("ed615b7a-b551-4a2d-aa12-56154c1c44aa"), relationship("books")),
                                        data(resource(
                                                type("book"),
                                                lid("f950e6f7-d392-4671-bacd-80966fa3ed5c")
                                        ), resource(
                                                type("book"),
                                                lid("d4fd57f0-3127-4bb6-b4c9-13bc78341737")
                                        ))
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        String result = response.asString();
        String expected = """
                {"atomic:results":[{"data":{"type":"book","id":"1","attributes":{"title":"Book 1"},"relationships":{"publisher":{"links":{"self":"https://elide.io/json/book/1/relationships/publisher","related":"https://elide.io/json/book/1/publisher"},"data":{"type":"publisher","id":"1"}}},"links":{"self":"https://elide.io/json/book/1"}}},{"data":{"type":"book","id":"2","attributes":{"title":"Book 2 Updated"},"relationships":{"publisher":{"links":{"self":"https://elide.io/json/book/2/relationships/publisher","related":"https://elide.io/json/book/2/publisher"},"data":{"type":"publisher","id":"1"}}},"links":{"self":"https://elide.io/json/book/2"}}},{"data":{"type":"publisher","id":"1","attributes":{"name":"Publisher"},"relationships":{"books":{"links":{"self":"https://elide.io/json/publisher/1/relationships/books","related":"https://elide.io/json/publisher/1/books"},"data":[{"type":"book","id":"1"},{"type":"book","id":"2"}]}},"links":{"self":"https://elide.io/json/publisher/1"}}},{"data":null},{"data":null}]}""";
        assertEquals(expected, result);
        String bookId1 = response.path("'atomic:results'[0].data.id");
        String bookId2 = response.path("'atomic:results'[1].data.id");
        String publisherId = response.path("'atomic:results'[2].data.id");

        ExtractableResponse<Response> deleteResponse = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.remove,
                                        ref(
                                           type("book"),
                                           id(bookId1)
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.remove,
                                        ref(
                                                type("book"),
                                                id(bookId2)
                                             )
                                ),
                                atomicOperation(AtomicOperationCode.remove,
                                        ref(
                                                type("publisher"),
                                                id(publisherId)
                                             )
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        String deleteResult = deleteResponse.asString();
        String deleteExpected = """
                {"atomic:results":[{"data":null},{"data":null},{"data":null}]}""";
        assertEquals(deleteExpected, deleteResult);
    }


    @Test
    public void jsonApiAtomicOperationsExtensionRelationshipTest() {
        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.add,
                                        datum(resource(
                                                type("group"),
                                                id("com.example.operationsrel1"),
                                                attributes(
                                                        attr("commonName", "Foo1")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.add,
                                        datum(resource(
                                                type("maintainer"),
                                                id("com.example.person1"),
                                                attributes(
                                                        attr("commonName", "Person1")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.add, "/group/com.example.operationsrel1/products",
                                        datum(resource(
                                                type("product"),
                                                id("com.example.operations.product1"),
                                                attributes(
                                                        attr("commonName", "Product1")
                                                )
                                        ))
                                ),
                                atomicOperation(AtomicOperationCode.add,
                                        "/group/com.example.operationsrel1/products/com.example.operations.product1/relationships/maintainers",
                                        data(resource(
                                                type("maintainer"),
                                                id("com.example.person1")
                                        ))
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        String result = response.asString();
        String expected = """
                {"atomic:results":[{"data":{"type":"group","id":"com.example.operationsrel1","attributes":{"commonName":"Foo1","deprecated":false,"description":""},"relationships":{"products":{"links":{"self":"https://elide.io/json/group/com.example.operationsrel1/relationships/products","related":"https://elide.io/json/group/com.example.operationsrel1/products"},"data":[{"type":"product","id":"com.example.operations.product1"}]}},"links":{"self":"https://elide.io/json/group/com.example.operationsrel1"}}},{"data":{"type":"maintainer","id":"com.example.person1","attributes":{"commonName":"Person1","description":""},"relationships":{"products":{"links":{"self":"https://elide.io/json/maintainer/com.example.person1/relationships/products","related":"https://elide.io/json/maintainer/com.example.person1/products"},"data":[]}},"links":{"self":"https://elide.io/json/maintainer/com.example.person1"}}},{"data":{"type":"product","id":"com.example.operations.product1","attributes":{"commonName":"Product1","description":""},"relationships":{"group":{"links":{"self":"https://elide.io/json/group/com.example.operationsrel1/products/com.example.operations.product1/relationships/group","related":"https://elide.io/json/group/com.example.operationsrel1/products/com.example.operations.product1/group"},"data":{"type":"group","id":"com.example.operationsrel1"}},"maintainers":{"links":{"self":"https://elide.io/json/group/com.example.operationsrel1/products/com.example.operations.product1/relationships/maintainers","related":"https://elide.io/json/group/com.example.operationsrel1/products/com.example.operations.product1/maintainers"},"data":[{"type":"maintainer","id":"com.example.person1"}]},"versions":{"links":{"self":"https://elide.io/json/group/com.example.operationsrel1/products/com.example.operations.product1/relationships/versions","related":"https://elide.io/json/group/com.example.operationsrel1/products/com.example.operations.product1/versions"},"data":[]}},"links":{"self":"https://elide.io/json/group/com.example.operationsrel1/products/com.example.operations.product1"}}},{"data":null}]}""";
        assertEquals(expected, result);

        ExtractableResponse<Response> deleteResponse = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.remove,
                                        "/group/com.example.operationsrel1/products/com.example.operations.product1/relationships/maintainers",
                                        data(resource(type("maintainer"), id("com.example.person1")))
                                ),
                                atomicOperation(AtomicOperationCode.remove,
                                        ref(
                                                type("group"),
                                                lid("com.example.operationsrel1"),
                                                relationship("products")
                                        ),
                                        data(resource(type("product"), id("com.example.operations.product1")))
                                ),
                                atomicOperation(AtomicOperationCode.remove,
                                        ref(
                                           type("maintainer"),
                                           id("com.example.person1")
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.remove,
                                        ref(
                                           type("group"),
                                           id("com.example.operationsrel1")
                                        )
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        String deleteResult = deleteResponse.asString();
        String deleteExpected = """
                {"atomic:results":[{"data":null},{"data":null},{"data":null},{"data":null}]}""";
        assertEquals(deleteExpected, deleteResult);
    }

    @Test
    public void jsonApiAtomicOperationsExtensionMissingRefTypeTest() {
        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.add,
                                        ref(
                                                type(null),
                                                id("com.example.operations.product1"),
                                                relationship("maintainers")
                                        )
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract();
        Map<String, Object> attributes = response.path("[0].errors[0]");
        assertThat(attributes).extractingByKeys("detail", "status").contains(
                "Bad Request Body&#39;Atomic Operations extension ref must specify the type member.&#39;", "400");
    }

    @Test
    public void jsonApiAtomicOperationsExtensionMissingRefAndHrefTest() {
        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.add,
                                        (String) null, null
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract();
        Map<String, Object> attributes = response.path("[0].errors[0]");
        assertThat(attributes).extractingByKeys("detail", "status").contains(
                "Bad Request Body&#39;Atomic Operations extension operation requires either ref or href members to be specified.&#39;", "400");
    }

    @Test
    public void jsonApiAtomicOperationsExtensionUnsupportedOpTest() {
        String body = """
                {
                  "atomic:operations": [{
                    "op": "<script src=''></script>",
                    "ref": {
                      "type": "articles",
                      "id": "1",
                      "relationship": "comments"
                    },
                    "data": [
                      { "type": "comments", "id": "123" }
                    ]
                  }]
                }
                            """;

        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(body)
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract();
        Map<String, Object> attributes = response.path("errors[0]");
        assertThat(attributes).extractingByKeys("detail").contains(
                "Bad Request Body&#39;Invalid Atomic Operations extension operation code:&lt;script src=&#39;&#39;&gt;&lt;/script&gt;&#39;");
    }

    @Test
    public void jsonApiAtomicOperationsExtensionMissingOpTest() {
        String body = """
                {
                  "atomic:operations": [{
                    "ref": {
                      "type": "articles",
                      "id": "1",
                      "relationship": "comments"
                    },
                    "data": [
                      { "type": "comments", "id": "123" }
                    ]
                  }]
                }
                      """;

        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(body)
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract();
        Map<String, Object> attributes = response.path("[0].errors[0]");
        assertThat(attributes).extractingByKeys("detail", "status").contains(
                "Bad Request Body&#39;Atomic Operations extension operation code must be specified.&#39;", "400");
    }

    @Test
    public void jsonApiAtomicOperationsExtensionMissingOperationsTest() {
        String body = """
                {
                  "atomic:operations": [null]
                }
                      """;

        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(body)
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract();
        Map<String, Object> attributes = response.path("[0].errors[0]");
        assertThat(attributes).extractingByKeys("detail", "status").contains(
                "Bad Request Body&#39;Atomic Operations extension operation must be specified.&#39;", "400");
    }

    @Test
    public void jsonApiAtomicOperationsExtensionInvalidFormatTest() {
        String body = """
                {"<script src=''></script>"}""";
        ExtractableResponse<Response> response = given()
                .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
                .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
                .body(body)
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract();
        Map<String, Object> attributes = response.path("errors[0]");
        assertThat(attributes).extractingByKeys("detail").contains(
                "Bad Request Body&#39;{&#34;&lt;script src=&#39;&#39;&gt;&lt;/script&gt;&#34;}&#39;");
    }

    @Test
    public void jsonApiPostTest() {
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        datum(
                                resource(
                                        type("group"),
                                        id("com.example.repository2"),
                                        attributes(
                                                attr("commonName", "New group.")
                                        )
                                )
                        )
                )
                .when()
                .post("/json/group")
                .then()
                .body(equalTo(datum(
                        resource(
                                type("group"),
                                id("com.example.repository2"),
                                attributes(
                                        attr("commonName", "New group."),
                                        attr("deprecated", false),
                                        attr("description", "")
                                ),
                                links(
                                        attr("self", baseUrl + "group/com.example.repository2")
                                ),
                                relationships(
                                        relation(
                                                "products",
                                                links(
                                                        attr("self", baseUrl + "group/com.example.repository2/relationships/products"),
                                                        attr("related", baseUrl + "group/com.example.repository2/products")
                                                )
                                        )
                                )
                        )
                ).toJSON()))
                .statusCode(HttpStatus.SC_CREATED);
    }

    @Test
    public void jsonApiDeleteTest() {
        when()
                .delete("/json/group/com.example.repository")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Function to verify the content type of a HTTP Delete error request.
     */
    @Test
    public void jsonApiDeleteErrorTest() {
        when()
                .delete("/json/group/doestnotexist")
                .then()
                .contentType(JsonApi.MEDIA_TYPE)
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    @Sql(statements = {
            "INSERT INTO ArtifactProduct (name, commonName, description, group_name) VALUES\n"
                    + "\t\t('foo','foo Core','The guts of foo','com.example.repository');"
    })
    public void jsonApiDeleteRelationshipTest() {
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(datum(
                        linkage(type("product"), id("foo"))
                ))
                .when()
                .delete("/json/group/com.example.repository")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * This test demonstrates an example test using the GraphQL DSL.
     */
    @Test
    public void graphqlTest() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
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
                .post("/graphql")
                .then()
                .body(equalTo(GraphQLDSL.document(
                        selection(
                                field(
                                        "group",
                                        selections(
                                                field("name", "com.example.repository"),
                                                field("commonName", "Example Repository"),
                                                field("description", "The code for this project")
                                        )
                                )
                        )
                ).toResponse()))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testInvalidApiVersion() {

        String graphQLRequest = GraphQLDSL.document(
                selection(
                        field(
                                "group",
                                selections(
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = """
                {"errors":[{"message":"Invalid operation: Invalid API Version","extensions":{"classification":"DataFetchingException"}}]}""";

        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header("ApiVersion", "2.0")
                .body("{ \"query\" : \"" + graphQLRequest + "\" }")
                .post("/graphql")
                .then()
                .body(equalTo(expected))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * This test demonstrates an example test using the GraphQL DSL.
     */
    @Test
    public void versionedGraphqlTest() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header("ApiVersion", "1.0")
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        query(
                                selection(
                                        field("group",
                                                selections(
                                                        field("name"),
                                                        field("title")
                                                )
                                        )
                                )
                        )
                        ).toQuery() + "\" }"
                )
                .when()
                .post("/graphql")
                .then()
                .body(equalTo(GraphQLDSL.document(
                        selection(
                                field(
                                        "group",
                                        selections(
                                                field("name", "com.example.repository"),
                                                field("title", "Example Repository")
                                        )
                                )
                        )
                ).toResponse()))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void apiDocsDocumentTest() {
        when()
                .get("/doc")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("tags.name", containsInAnyOrder("group", "argument", "metric",
                        "dimension", "column", "table", "asyncQuery",
                        "timeDimensionGrain", "timeDimension", "product", "playerCountry", "version", "playerStats",
                        "stats", "namespace", "tableSource", "maintainer", "book", "publisher", "person",
                        "export"));
    }

    @Test
    public void apiDocsDocumentTestYaml() throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        String response = given()
                .accept("application/yaml")
                .when()
                .get("/doc")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().asString();
        JsonNode jsonNode = objectMapper.readTree(response);
        JsonNode tags = jsonNode.get("tags");
        assertTrue(tags.isArray());
    }


    @Test
    public void versionedApiDocsDocumentTest() {
        ExtractableResponse<Response> v0 = given()
                .when()
                .get("/doc")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();

        ExtractableResponse<Response> v1 = given()
                .header("ApiVersion", "1.0")
                .when()
                .get("/doc")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        assertNotEquals(v0.asString(), v1.asString());
        assertNull(v0.path("info.version"));
        assertEquals("1.0", v1.path("info.version"));

        given()
                .header("ApiVersion", "2.0")
                .when()
                .get("/doc")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("[]"));
    }

    @Test
    public void apiDocsXSSDocumentTest() {
        when()
                .get("/doc/<script>")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(equalTo("Unknown document: &lt;script&gt;"));
    }

    @Test
    public void apiDocsXSSDocumentTestYaml() {
        given()
                .accept("application/yaml")
                .when()
                .get("/doc/<script>")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(equalTo("Unknown document: &lt;script&gt;"));
    }

    @Test
    public void graphqlTestForbiddenCreate() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        mutation(
                                selection(
                                        field("group",
                                                arguments(
                                                        argument("op", "UPSERT"),
                                                        argument("data", "{name:\\\"Foo\\\", deprecated:true}")
                                                ),
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
                .post("/graphql")
                .then()
                .body("errors.message", contains("CreatePermission Denied"))
                .statusCode(200);
    }

    // Controller disabled by default.
    @Test
    public void exportControllerDisabledTest() {
        // Though post is not supported for export we can use it to test if controller is disabled.
        // post returns with 404 if controller is disabled and 405 when enabled.
        when()
                .post("/export/asyncQueryId")
                .then()
                .body("error", equalTo("Not Found"))
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
