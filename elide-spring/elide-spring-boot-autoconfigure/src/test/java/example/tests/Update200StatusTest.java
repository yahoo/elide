/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static io.restassured.RestAssured.given;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.jsonapi.JsonApi;
import com.paiondata.elide.test.jsonapi.JsonApiDSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

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
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        JsonApiDSL.datum(
                                JsonApiDSL.resource(
                                        JsonApiDSL.type("group"),
                                        JsonApiDSL.id("com.example.repository"),
                                        JsonApiDSL.attributes(
                                                JsonApiDSL.attr("commonName", "Changed It.")
                                        )
                                )
                        )
                )
                .when()
                .patch("/json/group/com.example.repository")
                .then()
                .contentType(JsonApi.MEDIA_TYPE)
                .statusCode(HttpStatus.SC_OK);
    }
}
