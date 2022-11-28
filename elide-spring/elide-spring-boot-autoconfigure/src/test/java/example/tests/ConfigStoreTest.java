/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.yahoo.elide.test.graphql.GraphQLDSL.argument;
import static com.yahoo.elide.test.graphql.GraphQLDSL.arguments;
import static com.yahoo.elide.test.graphql.GraphQLDSL.field;
import static com.yahoo.elide.test.graphql.GraphQLDSL.mutation;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.test.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType;
import com.yahoo.elide.spring.controllers.JsonApiController;
import com.yahoo.elide.test.graphql.GraphQLDSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import io.restassured.RestAssured;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.TimeZone;

import javax.ws.rs.core.MediaType;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ConfigStoreIntegrationTestSetup.class)
@TestPropertySource(
        properties = {
                "elide.dynamic-config.configApiEnabled=true"
        }
)
public class ConfigStoreTest {

    @Data
    @Builder
    public static class ConfigFile {
        ConfigFileType type;

        String path;

        String content;
    }

    @LocalServerPort
    protected int port;

    @BeforeAll
    public static void initialize(@TempDir Path testDirectory) {
        System.setProperty("elide.dynamic-config.path", testDirectory.toFile().getAbsolutePath());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    public static void cleanup() {
        System.clearProperty("elide.dynamic-config.path");
    }

    /**
     * Empty configuration load test.
     */
    @Test
    public void testEmptyConfiguration() {
        when()
                .get("http://localhost:" + port + "/json/config?fields[config]=path,type")
                .then()
                .body(equalTo("{\"data\":[]}"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testGraphQLNullContent() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        mutation(
                                selection(
                                        field("config",
                                                arguments(
                                                        argument("op", "UPSERT"),
                                                        argument("data", "{ type: TABLE, path: \\\"models/tables/table1.hjson\\\" }")
                                                ),
                                                selections(
                                                        field("id"),
                                                        field("path")
                                                )
                                        )
                                )
                        )
                ).toQuery() + "\" }")
                .when()
                .post("http://localhost:" + port + "/graphql")
                .then()
                .body(equalTo("{\"errors\":[{\"message\":\"Null or empty file content for models/tables/table1.hjson\"}]}"))
                .statusCode(200);
    }

    @Test
    public void testGraphQLCreateFetchAndDelete() {
        String hjson = "\\\"{\\\\n"
                + "  tables: [{\\\\n"
                + "      name: Test\\\\n"
                + "      table: test\\\\n"
                + "      schema: test\\\\n"
                + "      measures : [\\\\n"
                + "         {\\\\n"
                + "          name : measure\\\\n"
                + "          type : INTEGER\\\\n"
                + "          definition: 'MAX({{$measure}})'\\\\n"
                + "         }\\\\n"
                + "      ]\\\\n"
                + "      dimensions : [\\\\n"
                + "         {\\\\n"
                + "           name : dimension\\\\n"
                + "           type : TEXT\\\\n"
                + "           definition : '{{$dimension}}'\\\\n"
                + "         }\\\\n"
                + "      ]\\\\n"
                + "  }]\\\\n"
                + "}\\\"";

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        mutation(
                                selection(
                                        field("config",
                                                arguments(
                                                        argument("op", "UPSERT"),
                                                        argument("data", String.format("{ type: TABLE, path: \\\"models/tables/table1.hjson\\\", content: %s }", hjson))
                                                ),
                                                selections(
                                                        field("id"),
                                                        field("path")
                                                )
                                        )
                                )
                        )
                ).toQuery() + "\" }")
                .when()
                .post("http://localhost:" + port + "/graphql")
                .then()
                .body(equalTo(
                        GraphQLDSL.document(
                                selection(
                                        field(
                                                "config",
                                                selections(
                                                        field("id", "bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24="),
                                                        field("path", "models/tables/table1.hjson")
                                                )
                                        )
                                )
                        ).toResponse()
               ))
               .statusCode(200);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        selection(
                                field("config",
                                        argument(
                                                argument("ids", "[\\\"bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=\\\"]")
                                        ),
                                        selections(
                                                field("id"),
                                                field("path")
                                        )
                                )
                        )
                ).toQuery() + "\" }")
                .when()
                .post("http://localhost:" + port + "/graphql")
                .then()
                .body(equalTo(
                        GraphQLDSL.document(
                                selection(
                                        field(
                                                "config",
                                                selections(
                                                        field("id", "bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24="),
                                                        field("path", "models/tables/table1.hjson")
                                                )
                                        )
                                )
                        ).toResponse()
                ))
                .statusCode(200);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        mutation(
                            selection(
                                    field("config",
                                            arguments(
                                                    argument("op", "DELETE"),
                                                    argument("ids", "[\\\"bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=\\\"]")
                                            ),
                                            selections(
                                                    field("id"),
                                                    field("path")
                                            )
                                    )
                            )
                        )
                ).toQuery() + "\" }")
                .when()
                .post("http://localhost:" + port + "/graphql")
                .then()
                .body(equalTo("{\"data\":{\"config\":{\"edges\":[]}}}"))
                .statusCode(200);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        selection(
                                field("config",
                                        selections(
                                                field("id"),
                                                field("path")
                                        )
                                )
                        )
                ).toQuery() + "\" }")
                .when()
                .post("http://localhost:" + port + "/graphql")
                .then()
                .body(equalTo("{\"data\":{\"config\":{\"edges\":[]}}}"))
                .statusCode(200);
    }

    @Test
    public void testTwoNamespaceCreateAndDelete() {
        String hjson1 = "\\\"{\\\\n  namespaces:\\\\n  [\\\\n    {\\\\n      name: DemoNamespace2\\\\n      description: Namespace for Demo Purposes\\\\n      friendlyName: Demo Namespace\\\\n    }\\\\n  ]\\\\n}\\\"";

        String hjson2 = "\\\"{\\\\n  namespaces:\\\\n  [\\\\n    {\\\\n      name: DemoNamespace3\\\\n      description: Namespace for Demo Purposes\\\\n      friendlyName: Demo Namespace\\\\n    }\\\\n  ]\\\\n}\\\"";

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        mutation(
                                selection(
                                        field("config",
                                                arguments(
                                                        argument("op", "UPSERT"),
                                                        argument("data", String.format("["
                                                                        + "{ type: NAMESPACE, path: \\\"models/namespaces/namespace2.hjson\\\", content: %s },"
                                                                        + "{ type: NAMESPACE, path: \\\"models/namespaces/namespace3.hjson\\\", content: %s }"
                                                                        + "]" , hjson1, hjson2))
                                                ),
                                                selections(
                                                        field("id"),
                                                        field("path")
                                                )
                                        )
                                )
                        )
                ).toQuery() + "\" }")
                .when()
                .post("http://localhost:" + port + "/graphql")
                .then()
                .body(equalTo(
                        GraphQLDSL.document(
                                selection(
                                        field(
                                                "config",
                                                selections(
                                                        field("id", "bW9kZWxzL25hbWVzcGFjZXMvbmFtZXNwYWNlMi5oanNvbg=="),
                                                        field("path", "models/namespaces/namespace2.hjson")
                                                ),
                                                selections(
                                                        field("id", "bW9kZWxzL25hbWVzcGFjZXMvbmFtZXNwYWNlMy5oanNvbg=="),
                                                        field("path", "models/namespaces/namespace3.hjson")
                                                )
                                        )
                                )
                        ).toResponse()
                ))
                .statusCode(200);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        mutation(
                                selection(
                                        field("config",
                                                arguments(
                                                        argument("op", "DELETE"),
                                                        argument("ids", "[\\\"bW9kZWxzL25hbWVzcGFjZXMvbmFtZXNwYWNlMi5oanNvbg==\\\", \\\"bW9kZWxzL25hbWVzcGFjZXMvbmFtZXNwYWNlMy5oanNvbg==\\\"]")
                                                ),
                                                selections(
                                                        field("id"),
                                                        field("path")
                                                )
                                        )
                                )
                        )
                ).toQuery() + "\" }")
                .when()
                .post("http://localhost:" + port + "/graphql")
                .then()
                .body(equalTo("{\"data\":{\"config\":{\"edges\":[]}}}"))
                .statusCode(200);

    }

    @Test
    public void testTwoNamespaceCreationStatements() {
        String query = "{ \"query\": \" mutation saveChanges {\\n  one: config(op: UPSERT, data: {id:\\\"one\\\", path: \\\"models/namespaces/oneDemoNamespaces.hjson\\\", type: NAMESPACE, content: \\\"{\\\\n  namespaces:\\\\n  [\\\\n    {\\\\n      name: DemoNamespace2\\\\n      description: Namespace for Demo Purposes\\\\n      friendlyName: Demo Namespace\\\\n    }\\\\n  ]\\\\n}\\\"}) {\\n    edges {\\n      node {\\n        id\\n      }\\n    }\\n  }\\n  two: config(op: UPSERT, data: {id: \\\"two\\\", path: \\\"models/namespaces/twoDemoNamespaces.hjson\\\", type: NAMESPACE, content: \\\"{\\\\n  namespaces:\\\\n  [\\\\n    {\\\\n      name: DemoNamespace3\\\\n      description: Namespace for Demo Purposes\\\\n      friendlyName: Demo Namespace\\\\n    }\\\\n  ]\\\\n}\\\"}) {\\n    edges {\\n      node {\\n        id\\n      }\\n    }\\n  }\\n} \" }";
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .when()
                .post("http://localhost:" + port + "/graphql")
                .then()
                .body(equalTo(
                        GraphQLDSL.document(
                                selections(
                                        field(
                                                "one",
                                                selections(
                                                        field("id", "bW9kZWxzL25hbWVzcGFjZXMvb25lRGVtb05hbWVzcGFjZXMuaGpzb24=")
                                                )
                                        ),
                                        field(
                                                "two",
                                                selections(
                                                        field("id", "bW9kZWxzL25hbWVzcGFjZXMvdHdvRGVtb05hbWVzcGFjZXMuaGpzb24=")
                                                )
                                        )
                                )
                        ).toResponse().replace(" ", "")
                ))
                .statusCode(200);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body("{ \"query\" : \"" + GraphQLDSL.document(
                        mutation(
                                selection(
                                        field("config",
                                                arguments(
                                                        argument("op", "DELETE"),
                                                        argument("ids", "[\\\"bW9kZWxzL25hbWVzcGFjZXMvb25lRGVtb05hbWVzcGFjZXMuaGpzb24=\\\", \\\"bW9kZWxzL25hbWVzcGFjZXMvdHdvRGVtb05hbWVzcGFjZXMuaGpzb24=\\\"]")
                                                ),
                                                selections(
                                                        field("id"),
                                                        field("path")
                                                )
                                        )
                                )
                        )
                ).toQuery() + "\" }")
                .when()
                .post("http://localhost:" + port + "/graphql")
                .then()
                .body(equalTo("{\"data\":{\"config\":{\"edges\":[]}}}"))
                .statusCode(200);

    }

    @Test
    public void testJsonApiCreateFetchAndDelete() {
        String hjson = "{            \n"
                + "  tables: [{     \n"
                + "      name: Test\n"
                + "      table: test\n"
                + "      schema: test\n"
                + "      measures : [\n"
                + "         {\n"
                + "          name : measure\n"
                + "          type : INTEGER\n"
                + "          definition: 'MAX({{$measure}})'\n"
                + "         }\n"
                + "      ]      \n"
                + "      dimensions : [\n"
                + "         {\n"
                + "           name : dimension\n"
                + "           type : TEXT\n"
                + "           definition : '{{$dimension}}'\n"
                + "         }\n"
                + "      ]\n"
                + "  }]\n"
                + "}";

        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("config"),
                                        attributes(
                                                attr("path", "models/tables/table1.hjson"),
                                                attr("type", "TABLE"),
                                                attr("content", hjson)
                                        )
                                )
                        )
                )
                .when()
                .post("http://localhost:" + port + "/json/config")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        when()
                .get("http://localhost:" + port + "/json/config?fields[config]=content")
                .then()
                .body(equalTo(data(
                        resource(
                                type("config"),
                                id("bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24="),
                                attributes(
                                        attr("content", hjson)
                                )
                        )
                ).toJSON()))
                .statusCode(HttpStatus.SC_OK);

        when()
                .delete("http://localhost:" + port + "/json/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        when()
                .get("http://localhost:" + port + "/json/config?fields[config]=path,type")
                .then()
                .body(equalTo("{\"data\":[]}"))
                .statusCode(HttpStatus.SC_OK);

        when()
                .get("http://localhost:" + port + "/json/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testUpdatePermissionError() {
        String hjson = "{            \n"
                + "  tables: [{     \n"
                + "      name: Test\n"
                + "      table: test\n"
                + "      schema: test\n"
                + "      measures : [\n"
                + "         {\n"
                + "          name : measure\n"
                + "          type : INTEGER\n"
                + "          definition: 'MAX({{$measure}})'\n"
                + "         }\n"
                + "      ]      \n"
                + "      dimensions : [\n"
                + "         {\n"
                + "           name : dimension\n"
                + "           type : TEXT\n"
                + "           definition : '{{$dimension}}'\n"
                + "         }\n"
                + "      ]\n"
                + "  }]\n"
                + "}";

        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("config"),
                                        attributes(
                                                attr("path", "models/tables/table1.hjson"),
                                                attr("type", "TABLE"),
                                                attr("content", hjson)
                                        )
                                )
                        )
                )
                .when()
                .post("http://localhost:" + port + "/json/config")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("config"),
                                        id("bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24="),
                                        attributes(
                                                attr("path", "models/tables/table1.hjson"),
                                                attr("type", "TABLE"),
                                                attr("content", hjson)
                                        )
                                )
                        )
                )
                .when()
                .patch("http://localhost:" + port + "/json/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        when()
                .delete("http://localhost:" + port + "/json/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testTemplateError() {
        String hjson = "{            \n"
                + "  tables: [{     \n"
                + "      name: Test\n"
                + "      table: test\n"
                + "      schema: test\n"
                + "      measures : [\n"
                + "         {\n"
                + "          name : measure\n"
                + "          type : INTEGER\n"
                + "          definition: 'MAX({{$measure}}) + {{$$column.args.missing}}'\n"
                + "         }\n"
                + "      ]      \n"
                + "      dimensions : [\n"
                + "         {\n"
                + "           name : dimension\n"
                + "           type : TEXT\n"
                + "           definition : '{{$dimension}}'\n"
                + "         }\n"
                + "      ]\n"
                + "  }]\n"
                + "}";

        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("config"),
                                        attributes(
                                                attr("path", "models/tables/table1.hjson"),
                                                attr("type", "TABLE"),
                                                attr("content", hjson)
                                        )
                                )
                        )
                )
                .when()
                .post("http://localhost:" + port + "/json/config")
                .then()
                .body(equalTo("{\"errors\":[{\"detail\":\"Failed to verify column arguments for column: measure in table: Test. Argument &#39;missing&#39; is not defined but found &#39;{{$$column.args.missing}}&#39;.\"}]}"))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testPathUpdatePermissionError() {
        String hjson = "{            \n"
                + "  tables: [{     \n"
                + "      name: Test\n"
                + "      table: test\n"
                + "      schema: test\n"
                + "      measures : [\n"
                + "         {\n"
                + "          name : measure\n"
                + "          type : INTEGER\n"
                + "          definition: 'MAX({{$measure}})'\n"
                + "         }\n"
                + "      ]      \n"
                + "      dimensions : [\n"
                + "         {\n"
                + "           name : dimension\n"
                + "           type : TEXT\n"
                + "           definition : '{{$dimension}}'\n"
                + "         }\n"
                + "      ]\n"
                + "  }]\n"
                + "}";

        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("config"),
                                        attributes(
                                                attr("path", "models/tables/table1.hjson"),
                                                attr("type", "TABLE"),
                                                attr("content", hjson)
                                        )
                                )
                        )
                )
                .when()
                .post("http://localhost:" + port + "/json/config")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("config"),
                                        id("bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24="),
                                        attributes(
                                                attr("path", "models/tables/newName.hjson")
                                        )
                                )
                        )
                )
                .when()
                .patch("http://localhost:" + port + "/json/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        when()
                .delete("http://localhost:" + port + "/json/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testHackAttempt() {
        String hjson = "#!/bin/sh ...";

        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("config"),
                                        attributes(
                                                attr("path", "foo"),
                                                attr("type", "UNKNOWN"),
                                                attr("content", hjson)
                                        )
                                )
                        )
                )
                .when()
                .post("http://localhost:" + port + "/json/config")
                .then()
                .body(equalTo("{\"errors\":[{\"detail\":\"Unrecognized File: foo\"}]}"))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testPathTraversalAttempt() {
        String hjson = "{            \n"
                + "  tables: [{     \n"
                + "      name: Test\n"
                + "      table: test\n"
                + "      schema: test\n"
                + "      measures : [\n"
                + "         {\n"
                + "          name : measure\n"
                + "          type : INTEGER\n"
                + "          definition: 'MAX({{$measure}})'\n"
                + "         }\n"
                + "      ]      \n"
                + "      dimensions : [\n"
                + "         {\n"
                + "           name : dimension\n"
                + "           type : TEXT\n"
                + "           definition : '{{$dimension}}'\n"
                + "         }\n"
                + "      ]\n"
                + "  }]\n"
                + "}";

        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("config"),
                                        attributes(
                                                attr("path", "../../../../../tmp/models/tables/table1.hjson"),
                                                attr("type", "TABLE"),
                                                attr("content", hjson)
                                        )
                                )
                        )
                )
                .when()
                .post("http://localhost:" + port + "/json/config")
                .then()
                .body(equalTo("{\"errors\":[{\"detail\":\"Parent directory traversal not allowed: ../../../../../tmp/models/tables/table1.hjson\"}]}"))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }
}
