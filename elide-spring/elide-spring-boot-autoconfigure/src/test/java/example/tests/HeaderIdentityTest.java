/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.jsonapi.JsonApi;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.HashMap;
import java.util.List;
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
@Import(IntegrationTestSetup.class)
@TestPropertySource(
        properties = {
                "elide.json-api.links.enabled=true",
                "elide.async.export.enabled=false",
                "elide.strip-authorization-headers=false",
                "spring.cloud.refresh.enabled=false"
        }
)
@ActiveProfiles("default")
class HeaderIdentityTest extends IntegrationTest {
    public static final String SORT_PARAM = "sort";

    @SpyBean
    private JsonApi jsonApi;

    @BeforeAll
    @Override
    public void setUp() {
        super.setUp();
    }

    @BeforeEach
    public void resetMocks() {
        reset(jsonApi);
    }

    @Test
    void jsonVerifyParamsAndHeadersGetTest() {
        given()
                .header(HttpHeaders.AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.PROXY_AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
                .queryParam(SORT_PARAM, "name", "description")
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
                .statusCode(HttpStatus.SC_CREATED);

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        verify(jsonApi).post(routeCaptor.capture(), any(), any(), any());

        Map<String, List<String>> expectedRequestParams = new HashMap<>();
        expectedRequestParams.put(SORT_PARAM, ImmutableList.of("name", "description"));
        assertEquals(expectedRequestParams, routeCaptor.getValue().getParameters());

        assertTrue(routeCaptor.getValue().getHeaders().containsKey("authorization"));
        assertTrue(routeCaptor.getValue().getHeaders().containsKey("proxy-authorization"));
    }

    @Test
    void jsonVerifyParamsAndHeadersPostTest() {
        given()
                .header(HttpHeaders.AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.PROXY_AUTHORIZATION, "willBeRemoved")
                .queryParam(SORT_PARAM, "name", "description")
                .when()
                .get("/json/group")
                .then()
                .statusCode(HttpStatus.SC_OK);

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        verify(jsonApi).get(routeCaptor.capture(), any(), any());

        Map<String, List<String>> expectedRequestParams = new HashMap<>();
        expectedRequestParams.put(SORT_PARAM, ImmutableList.of("name", "description"));
        assertEquals(expectedRequestParams, routeCaptor.getValue().getParameters());

        assertTrue(routeCaptor.getValue().getHeaders().containsKey("authorization"));
        assertTrue(routeCaptor.getValue().getHeaders().containsKey("proxy-authorization"));
    }

    @Test
    void jsonVerifyParamsAndHeadersPatchTest() {
        given()
                .header(HttpHeaders.AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.PROXY_AUTHORIZATION, "willBeRemoved")
                .queryParam(SORT_PARAM, "name", "description")
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

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        verify(jsonApi).patch(routeCaptor.capture(), any(), any(), any());

        Map<String, List<String>> expectedRequestParams = new HashMap<>();
        expectedRequestParams.put(SORT_PARAM, ImmutableList.of("name", "description"));
        assertEquals(expectedRequestParams, routeCaptor.getValue().getParameters());

        assertTrue(routeCaptor.getValue().getHeaders().containsKey("authorization"));
        assertTrue(routeCaptor.getValue().getHeaders().containsKey("proxy-authorization"));
    }

    @Test
    void jsonVerifyParamsAndHeadersDeleteTest() {
        given()
                .header(HttpHeaders.AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.PROXY_AUTHORIZATION, "willBeRemoved")
                .queryParam(SORT_PARAM, "name", "description")
                .when()
                .delete("/json/group/com.example.repository")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        verify(jsonApi).delete(
                routeCaptor.capture(),
                any(),
                any(),
                any()
        );

        Map<String, List<String>> expectedRequestParams = new HashMap<>();
        expectedRequestParams.put(SORT_PARAM, ImmutableList.of("name", "description"));
        assertEquals(expectedRequestParams, routeCaptor.getValue().getParameters());

        assertTrue(routeCaptor.getValue().getHeaders().containsKey("authorization"));
        assertTrue(routeCaptor.getValue().getHeaders().containsKey("proxy-authorization"));
    }

    @Test
    void jsonVerifyParamsAndHeadersDeleteRelationshipTest() {
        given()
                .header(HttpHeaders.AUTHORIZATION, "willBeRemoved")
                .header(HttpHeaders.PROXY_AUTHORIZATION, "willBeRemoved")
                .queryParam(SORT_PARAM, "name", "description")
                .contentType(JsonApi.MEDIA_TYPE)
                .body(datum(
                        linkage(type("product"), id("foo"))
                ))
                .when()
                .delete("/json/group/com.example.repository")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        verify(jsonApi).delete(
                routeCaptor.capture(),
                any(),
                any(),
                any()
        );

        Map<String, List<String>> expectedRequestParams = new HashMap<>();
        expectedRequestParams.put(SORT_PARAM, ImmutableList.of("name", "description"));
        assertEquals(expectedRequestParams, routeCaptor.getValue().getParameters());

        assertTrue(routeCaptor.getValue().getHeaders().containsKey("authorization"));
        assertTrue(routeCaptor.getValue().getHeaders().containsKey("proxy-authorization"));
    }
}
