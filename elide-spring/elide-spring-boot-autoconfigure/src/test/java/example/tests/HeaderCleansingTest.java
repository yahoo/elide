/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.spring.controllers.JsonApiController;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

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
@Import(IntegrationTestSetup.class)
@TestPropertySource(
        properties = {
                "elide.json-api.enableLinks=true",
                "elide.async.export.enabled=false",
        }
)
@ActiveProfiles("default")
public class HeaderCleansingTest extends IntegrationTest {
    public static final String SORT_PARAM = "sort";
    private String baseUrl;

    @SpyBean
    private RefreshableElide elide;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeAll
    @Override
    public void setUp() {
        super.setUp();
        baseUrl = "https://elide.io/json/";
    }

    @BeforeEach
    public void resetMocks() {
        reset(elide.getElide());
    }

    @Test
    public void jsonVerifyParamsAndHeadersGetTest() {
        given()
                .header(HttpHeaders.AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.PROXY_AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
                .queryParam(SORT_PARAM, "name", "description")
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
                .statusCode(HttpStatus.SC_CREATED);

        ArgumentCaptor<MultivaluedMap<String, String>> requestParamsCaptor = ArgumentCaptor.forClass(MultivaluedMap.class);
        ArgumentCaptor<Map<String, List<String>>> requestHeadersCleanedCaptor = ArgumentCaptor.forClass(Map.class);
        verify(elide.getElide()).post(any(), any(), any(), requestParamsCaptor.capture(), requestHeadersCleanedCaptor.capture(), any(), any(), any());

        MultivaluedHashMap<String, String> expectedRequestParams = new MultivaluedHashMap<>();
        expectedRequestParams.put(SORT_PARAM, ImmutableList.of("name", "description"));
        assertEquals(expectedRequestParams, requestParamsCaptor.getValue());

        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("authorization"));
        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("proxy-authorization"));
    }

    @Test
    public void jsonVerifyParamsAndHeadersPostTest() {
        given()
                .header(HttpHeaders.AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.PROXY_AUTHORIZATION, "willBeRemoved")
                .queryParam(SORT_PARAM, "name", "description")
                .when()
                .get("/json/group")
                .then()
                .statusCode(HttpStatus.SC_OK);

        ArgumentCaptor<MultivaluedMap<String, String>> requestParamsCaptor = ArgumentCaptor.forClass(MultivaluedMap.class);
        ArgumentCaptor<Map<String, List<String>>> requestHeadersCleanedCaptor = ArgumentCaptor.forClass(Map.class);
        verify(elide.getElide()).get(any(), any(), requestParamsCaptor.capture(), requestHeadersCleanedCaptor.capture(), any(), any(), any());

        MultivaluedHashMap<String, String> expectedRequestParams = new MultivaluedHashMap<>();
        expectedRequestParams.put(SORT_PARAM, ImmutableList.of("name", "description"));
        assertEquals(expectedRequestParams, requestParamsCaptor.getValue());

        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("authorization"));
        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("proxy-authorization"));
    }

    @Test
    public void jsonVerifyParamsAndHeadersPatchTest() {
        given()
                .header(HttpHeaders.AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.PROXY_AUTHORIZATION, "willBeRemoved")
                .queryParam(SORT_PARAM, "name", "description")
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

        ArgumentCaptor<MultivaluedMap<String, String>> requestParamsCaptor = ArgumentCaptor.forClass(MultivaluedMap.class);
        ArgumentCaptor<Map<String, List<String>>> requestHeadersCleanedCaptor = ArgumentCaptor.forClass(Map.class);
        verify(elide.getElide()).patch(any(), any(), any(), any(), any(), requestParamsCaptor.capture(), requestHeadersCleanedCaptor.capture(), any(), any(), any());

        MultivaluedHashMap<String, String> expectedRequestParams = new MultivaluedHashMap<>();
        expectedRequestParams.put(SORT_PARAM, ImmutableList.of("name", "description"));
        assertEquals(expectedRequestParams, requestParamsCaptor.getValue());

        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("authorization"));
        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("proxy-authorization"));
    }

    @Test
    public void jsonVerifyParamsAndHeadersDeleteTest() {
        given()
                .header(HttpHeaders.AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.PROXY_AUTHORIZATION, "willBeRemoved")
                .queryParam(SORT_PARAM, "name", "description")
                .when()
                .delete("/json/group/com.example.repository")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        ArgumentCaptor<MultivaluedMap<String, String>> requestParamsCaptor = ArgumentCaptor.forClass(MultivaluedMap.class);
        ArgumentCaptor<Map<String, List<String>>> requestHeadersCleanedCaptor = ArgumentCaptor.forClass(Map.class);
        verify(elide.getElide()).delete(
                any(),
                any(),
                any(),
                requestParamsCaptor.capture(),
                requestHeadersCleanedCaptor.capture(),
                any(),
                any(),
                any()
        );

        MultivaluedHashMap<String, String> expectedRequestParams = new MultivaluedHashMap<>();
        expectedRequestParams.put(SORT_PARAM, ImmutableList.of("name", "description"));
        assertEquals(expectedRequestParams, requestParamsCaptor.getValue());

        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("authorization"));
        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("proxy-authorization"));
    }

    @Test
    public void jsonVerifyParamsAndHeadersDeleteRelationshipTest() {
        given()
                .header(HttpHeaders.AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.PROXY_AUTHORIZATION, "willBeRemoved")
                .queryParam(SORT_PARAM, "name", "description")
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(datum(
                        linkage(type("product"), id("foo"))
                ))
                .when()
                .delete("/json/group/com.example.repository")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        ArgumentCaptor<MultivaluedMap<String, String>> requestParamsCaptor = ArgumentCaptor.forClass(MultivaluedMap.class);
        ArgumentCaptor<Map<String, List<String>>> requestHeadersCleanedCaptor = ArgumentCaptor.forClass(Map.class);
        verify(elide.getElide()).delete(
                any(),
                any(),
                any(),
                requestParamsCaptor.capture(),
                requestHeadersCleanedCaptor.capture(),
                any(),
                any(),
                any()
        );

        MultivaluedHashMap<String, String> expectedRequestParams = new MultivaluedHashMap<>();
        expectedRequestParams.put(SORT_PARAM, ImmutableList.of("name", "description"));
        assertEquals(expectedRequestParams, requestParamsCaptor.getValue());

        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("authorization"));
        assertFalse(requestHeadersCleanedCaptor.getValue().containsKey("proxy-authorization"));
    }
}
