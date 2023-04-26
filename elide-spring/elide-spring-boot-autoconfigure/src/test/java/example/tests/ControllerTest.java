/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.yahoo.elide.test.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.test.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.test.graphql.GraphQLDSL.field;
import static com.yahoo.elide.test.graphql.GraphQLDSL.mutation;
import static com.yahoo.elide.test.graphql.GraphQLDSL.query;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.atomicOperation;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.atomicOperations;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.links;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.patchOperation;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.patchSet;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.ref;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static com.yahoo.elide.test.jsonapi.elements.PatchOperationType.add;
import static com.yahoo.elide.test.jsonapi.elements.PatchOperationType.remove;
import static com.yahoo.elide.test.jsonapi.elements.PatchOperationType.replace;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.spring.controllers.JsonApiController;
import com.yahoo.elide.test.graphql.GraphQLDSL;
import com.yahoo.elide.test.jsonapi.elements.AtomicOperationCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import jakarta.ws.rs.core.MediaType;

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
                "elide.json-api.enableLinks=true",
                "elide.async.export.enabled=false",
        }
)
@ActiveProfiles("default")
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
    public void jsonApiPatchTest() {
        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
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
    public void jsonForbiddenApiPatchTest() {
        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
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
                .contentType(JsonApiController.JSON_API_PATCH_CONTENT_TYPE)
                .accept(JsonApiController.JSON_API_PATCH_CONTENT_TYPE)
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
        Map<String, Object> attributes = response.path("[1].data.attributes");
        assertEquals("Foo2", attributes.get("commonName"));
        assertEquals("Updated Description", attributes.get("description"));

        ExtractableResponse<Response> deleteResponse = given()
                .contentType(JsonApiController.JSON_API_PATCH_CONTENT_TYPE)
                .accept(JsonApiController.JSON_API_PATCH_CONTENT_TYPE)
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
        String result = deleteResponse.asString();
        String expected = """
                [{"data":null},{"data":null}]""";
        assertEquals(expected, result);
    }

    @Test
    public void jsonApiAtomicOperationsExtensionTest() {
        ExtractableResponse<Response> response = given()
                .contentType(JsonApiController.JSON_API_ATOMIC_OPERATIONS_CONTENT_TYPE)
                .accept(JsonApiController.JSON_API_ATOMIC_OPERATIONS_CONTENT_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.add, "/group",
                                        resource(
                                                type("group"),
                                                id("com.example.operations1"),
                                                attributes(
                                                        attr("commonName", "Foo1")
                                                )
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.add, "/group",
                                        resource(
                                                type("group"),
                                                id("com.example.operations2"),
                                                attributes(
                                                        attr("commonName", "Foo2")
                                                )
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.update, "/group/com.example.operations2",
                                        resource(
                                                type("group"),
                                                id("com.example.operations2"),
                                                attributes(
                                                        attr("description", "Updated Description")
                                                )
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.add, "/group/com.example.operations2/products",
                                        resource(
                                                type("product"),
                                                id("com.example.operations.product1"),
                                                attributes(
                                                        attr("commonName", "Product1")
                                                )
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.update, "/group/com.example.operations2/products/com.example.operations.product1",
                                        resource(
                                                type("product"),
                                                id("com.example.operations.product1"),
                                                attributes(
                                                        attr("description", "Product1 Description")
                                                )
                                        )
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        Map<String, Object> groupAttributes = response.path("'atomic:results'[1].data.attributes");
        assertEquals("Foo2", groupAttributes.get("commonName"));
        assertEquals("Updated Description", groupAttributes.get("description"));

        Map<String, Object> productAttributes = response.path("'atomic:results'[3].data.attributes");
        assertEquals("Product1", productAttributes.get("commonName"));
        assertEquals("Product1 Description", productAttributes.get("description"));


        ExtractableResponse<Response> deleteResponse = given()
                .contentType(JsonApiController.JSON_API_ATOMIC_OPERATIONS_CONTENT_TYPE)
                .accept(JsonApiController.JSON_API_ATOMIC_OPERATIONS_CONTENT_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.remove,
                                        resource(
                                                type("group"),
                                                id("com.example.operations1")
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.remove,
                                        resource(
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
        String result = deleteResponse.asString();
        String expected = """
                {"atomic:results":[{"data":null},{"data":null}]}""";
        assertEquals(expected, result);

    }

    @Test
    public void jsonApiAtomicOperationsExtensionPathInferTest() {
        ExtractableResponse<Response> response = given()
                .contentType(JsonApiController.JSON_API_ATOMIC_OPERATIONS_CONTENT_TYPE)
                .accept(JsonApiController.JSON_API_ATOMIC_OPERATIONS_CONTENT_TYPE)
                .body(
                        atomicOperations(
                                atomicOperation(AtomicOperationCode.add,
                                        resource(
                                                type("group"),
                                                id("com.example.operationsinfer1"),
                                                attributes(
                                                        attr("commonName", "Foo1")
                                                )
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.add,
                                        resource(
                                                type("group"),
                                                id("com.example.operationsinfer2"),
                                                attributes(
                                                        attr("commonName", "Foo2")
                                                )
                                        )
                                ),
                                atomicOperation(AtomicOperationCode.update,
                                        resource(
                                                type("group"),
                                                id("com.example.operationsinfer2"),
                                                attributes(
                                                        attr("description", "Updated Description")
                                                )
                                        )
                                )
                        )
                )
                .when()
                .post("/json/operations")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        Map<String, Object> attributes = response.path("'atomic:results'[1].data.attributes");
        assertEquals("Foo2", attributes.get("commonName"));
        assertEquals("Updated Description", attributes.get("description"));

        ExtractableResponse<Response> deleteResponse = given()
                .contentType(JsonApiController.JSON_API_ATOMIC_OPERATIONS_CONTENT_TYPE)
                .accept(JsonApiController.JSON_API_ATOMIC_OPERATIONS_CONTENT_TYPE)
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
        String result = deleteResponse.asString();
        String expected = """
                {"atomic:results":[{"data":null},{"data":null}]}""";
        assertEquals(expected, result);
    }

    @Test
    public void jsonApiPostTest() {
        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
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
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    @Sql(statements = {
            "INSERT INTO ArtifactProduct (name, commonName, description, group_name) VALUES\n"
                    + "\t\t('foo','foo Core','The guts of foo','com.example.repository');"
    })
    public void jsonApiDeleteRelationshipTest() {
        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
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

        String expected = "{\"errors\":[{\"message\":\"Invalid operation: Invalid API Version\"}]}";

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
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
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
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
                        "stats", "namespace", "tableSource"));
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
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
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
