/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.spring.controllers.JsonApiController;
import com.yahoo.elide.test.graphql.GraphQLDSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import javax.ws.rs.core.MediaType;

import static com.yahoo.elide.test.graphql.GraphQLDSL.*;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.*;
import static com.yahoo.elide.test.jsonapi.elements.PatchOperationType.add;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

/**
 * Verifies 200 Status for patch Requests.
 */
@Import(Update200StatusTestSetup.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = "classpath:db/test_init.sql",
        statements = "INSERT INTO ArtifactGroup (name, commonName, description, deprecated) VALUES\n"
                + "\t\t('com.example.repository','Example Repository','The code for this project', false);"
)
public class Update200StatusTest extends IntegrationTest {
    private String baseUrl;

    @BeforeAll
    @Override
    public void setUp() {
        super.setUp();
        baseUrl = "https://elide.io/json/";
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
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .statusCode(HttpStatus.SC_OK);
    }
}
