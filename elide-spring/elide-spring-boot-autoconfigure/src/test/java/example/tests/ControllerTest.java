/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.query;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.patchOperation;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.patchSet;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.elements.PatchOperationType.add;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.spring.controllers.JsonApiController;
import example.models.jpa.ArtifactGroup;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.io.IOException;
import javax.ws.rs.core.MediaType;

/**
 * Example functional test.
 */
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        statements = "INSERT INTO ArtifactGroup (name, commonName, description, deprecated) VALUES\n"
                + "\t\t('com.example.repository','Example Repository','The code for this project', false);")
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
        statements = "DELETE FROM ArtifactVersion; DELETE FROM ArtifactProduct; DELETE FROM ArtifactGroup;")
public class ControllerTest extends IntegrationTest {
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
                                        relationships(
                                                relation("products")
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
                                        relationships(
                                                relation("products")
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
        given()
                .contentType(JsonApiController.JSON_API_PATCH_CONTENT_TYPE)
                .accept(JsonApiController.JSON_API_PATCH_CONTENT_TYPE)
                .body(
                        patchSet(
                                patchOperation(add, "/group",
                                        resource(
                                                type("group"),
                                                id("com.example.repository.foo"),
                                                attributes(
                                                    attr("commonName", "Foo")
                                                )
                                        )
                                )
                        )
                )
                .when()
                .patch("/json")
                .then()
                .statusCode(HttpStatus.SC_OK);
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
                                relationships(
                                        relation("products")
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
    public void testInvalidApiVersion() throws IOException {

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
    public void swaggerDocumentTest() {
        when()
                .get("/doc")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("tags.name", containsInAnyOrder("group", "functionArgument", "metric",
                        "metricFunction", "dimension", "column", "table", "asyncQuery", "asyncQueryResult",
                        "timeDimensionGrain", "timeDimension"));
    }

    @Test
    public void versionedSwaggerDocumentTest() {
        given()
                .header("ApiVersion", "1.0")
        .when()
                .get("/doc")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("[]"));
    }

    @Test
    public void swaggerXSSDocumentTest() {
        when()
                .get("/doc/<script>")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(equalTo("Unknown document: &lt;script&gt;"));
    }

    @Test
    public void graphqlTestForbiddenCreate() {
        ArtifactGroup group = new ArtifactGroup();
        group.setDeprecated(true);
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        query(
                                selection(
                                        field("group",
                                                arguments(
                                                        argument("op", "UPSERT"),
                                                        argument("data", group)
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
}
