/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import static com.paiondata.elide.test.graphql.GraphQLDSL.argument;
import static com.paiondata.elide.test.graphql.GraphQLDSL.arguments;
import static com.paiondata.elide.test.graphql.GraphQLDSL.field;
import static com.paiondata.elide.test.graphql.GraphQLDSL.mutation;
import static com.paiondata.elide.test.graphql.GraphQLDSL.selection;
import static com.paiondata.elide.test.graphql.GraphQLDSL.selections;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.data;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.links;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.Injector;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.security.checks.prefab.Role;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.modelconfig.DynamicConfiguration;
import com.paiondata.elide.modelconfig.store.models.ConfigChecks;
import com.paiondata.elide.standalone.ElideStandalone;
import com.paiondata.elide.standalone.config.ElideStandaloneAnalyticSettings;
import com.paiondata.elide.standalone.config.ElideStandaloneSettings;
import com.paiondata.elide.test.graphql.GraphQLDSL;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.ws.rs.core.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneConfigStoreTest {
    protected ElideStandalone elide;
    protected ElideStandaloneSettings settings;
    protected Path configRoot;

    @AfterAll
    public void shutdown() throws Exception {
        configRoot.toFile().delete();
        elide.stop();
    }

    @BeforeAll
    public void init() throws Exception {
        configRoot = Paths.get(Files.createTempDirectory("test").toFile().getAbsolutePath(), "1", "2", "3", "4", "5"); // for path traversal attempt
        Files.createDirectories(configRoot);
        settings = new ElideStandaloneTestSettings() {

            @Override
            public EntityDictionary getEntityDictionary(ServiceLocator injector, ClassScanner scanner,
                                                        Optional<DynamicConfiguration> dynamicConfiguration, Set<Type<?>> entitiesToExclude) {

                Map<String, Class<? extends Check>> checks = new HashMap<>();

                if (getAnalyticProperties().enableDynamicModelConfigAPI()) {
                    checks.put(ConfigChecks.CAN_CREATE_CONFIG, ConfigChecks.CanCreate.class);
                    checks.put(ConfigChecks.CAN_READ_CONFIG, ConfigChecks.CanRead.class);
                    checks.put(ConfigChecks.CAN_DELETE_CONFIG, ConfigChecks.CanDelete.class);
                    checks.put(ConfigChecks.CAN_UPDATE_CONFIG, ConfigChecks.CanNotUpdate.class);
                }

                EntityDictionary dictionary = new EntityDictionary(
                        checks, //Checks
                        new HashMap<>(), //Role Checks
                        new Injector() {
                            @Override
                            public void inject(Object entity) {
                                injector.inject(entity);
                            }

                            @Override
                            public <T> T instantiate(Class<T> cls) {
                                return injector.create(cls);
                            }
                        },
                        CoerceUtil::lookup, //Serde Lookup
                        entitiesToExclude,
                        scanner);

                dynamicConfiguration.map(DynamicConfiguration::getRoles).orElseGet(Collections::emptySet).forEach(role ->
                        dictionary.addRoleCheck(role, new Role.RoleMemberCheck(role))
                );

                return dictionary;
            }

            @Override
            public ElideStandaloneAnalyticSettings getAnalyticProperties() {
                return new ElideStandaloneAnalyticSettings() {
                    @Override
                    public boolean enableDynamicModelConfig() {
                        return true;
                    }

                    @Override
                    public boolean enableDynamicModelConfigAPI() {
                        return true;
                    }

                    @Override
                    public String getDynamicConfigPath() {
                        return configRoot.toFile().getAbsolutePath();
                    }

                    @Override
                    public boolean enableAggregationDataStore() {
                        return true;
                    }

                    @Override
                    public boolean enableMetaDataStore() {
                        return true;
                    }
                };
            }
        };

        elide = new ElideStandalone(settings);
        elide.start(false);
    }

    /**
     * Empty configuration load test.
     */
    @Test
    public void testEmptyConfiguration() {
        when()
                .get("/api/config?fields[config]=path,type")
                .then()
                .body(equalTo("{\"data\":[]}"))
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testGraphQLNullContent() {
        String expected = """
                {"errors":[{"message":"Null or empty file content for models/tables/table1.hjson","extensions":{"classification":"DataFetchingException"}}]}""";
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
                .post("/graphql/api")
                .then()
                .body(equalTo(expected))
                .log().all()
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
                .post("/graphql/api")
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
                .post("/graphql/api")
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
                                                                + "]", hjson1, hjson2))
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
                .post("/graphql/api")
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
                .post("/graphql/api")
                .then()
                .body(equalTo("{\"data\":{\"config\":{\"edges\":[]}}}"))
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
                .post("/graphql/api")
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
                .post("/graphql/api")
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
                .post("/graphql/api")
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
                .post("/graphql/api")
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
                .contentType(JsonApi.MEDIA_TYPE)
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
                .post("/api/config")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        when()
                .get("/api/config?fields[config]=content")
                .then()
                .body(equalTo(data(
                        resource(
                                type("config"),
                                id("bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24="),
                                attributes(
                                        attr("content", hjson)
                                ),
                                links(
                                        attr("self", "https://elide.io/api/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                                )
                        )
                ).toJSON()))
                .statusCode(HttpStatus.SC_OK);

        when()
                .delete("/api/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        when()
                .get("/api/config?fields[config]=path,type")
                .then()
                .body(equalTo("{\"data\":[]}"))
                .statusCode(HttpStatus.SC_OK);

        when()
                .get("/api/json/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
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
                .contentType(JsonApi.MEDIA_TYPE)
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
                .post("/api/config")
                .then()
                .body(equalTo("{\"errors\":[{\"detail\":\"Failed to verify column arguments for column: measure in table: Test. Argument &#39;missing&#39; is not defined but found &#39;{{$$column.args.missing}}&#39;.\"}]}"))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testHackAttempt() {
        String hjson = "#!/bin/sh ...";

        given()
                .contentType(JsonApi.MEDIA_TYPE)
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
                .post("/api/config")
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
                .contentType(JsonApi.MEDIA_TYPE)
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
                .post("/api/config")
                .then()
                .body(equalTo("{\"errors\":[{\"detail\":\"Parent directory traversal not allowed: ../../../../../tmp/models/tables/table1.hjson\"}]}"))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
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
                .contentType(JsonApi.MEDIA_TYPE)
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
                .post("/api/config")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .contentType(JsonApi.MEDIA_TYPE)
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
                .patch("/api/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        when()
                .delete("/api/config/bW9kZWxzL3RhYmxlcy90YWJsZTEuaGpzb24=")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }
}
