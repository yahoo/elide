/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.async.DefaultResultTypeFileExtensionMapper;
import com.yahoo.elide.async.ResultTypeFileExtensionMapper;
import com.yahoo.elide.async.export.formatter.TableExportFormatter;
import com.yahoo.elide.async.export.formatter.TableExportFormatterContext;
import com.yahoo.elide.async.export.formatter.TableExportFormattersBuilderCustomizer;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.jsonapi.JsonApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import io.restassured.response.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Basic functional tests to test Async service setup, JSONAPI and GRAPHQL endpoints.
 */
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = "classpath:db/test_init.sql",
        statements = {
                "INSERT INTO ArtifactGroup (name, commonName, description, deprecated) VALUES\n"
                    + "\t\t('com.example.repository','Example Repository','The code for this project', false);",
                "INSERT INTO Stats (id, measure, dimension) VALUES\n"
                    + "\t\t(1,100,'Foo'),"
                    + "\t\t(2,150,'Bar');",
                "INSERT INTO PlayerStats (name, highScore, countryId, createdOn, updatedOn) VALUES\n"
                    + "\t\t('Sachin',100, 1, '2020-01-01', now());",
                "INSERT INTO PlayerCountry (id, isoCode) VALUES\n"
                    + "\t\t(1, 'IND');"
})
public class AsyncTest extends IntegrationTest {
    @TestConfiguration
    public static class TableExportFormatterConfiguration {
        public static class MyResultTypeFileExtensionMapper extends DefaultResultTypeFileExtensionMapper {
            @Override
            public String getFileExtension(String resultType) {
                switch (resultType) {
                case "XLSX":
                    return ".xlsx";
                default:
                    return super.getFileExtension(resultType);
                }
            }
        }

        public static class MyTableExportFormatter implements TableExportFormatter {
            @Override
            public void format(TableExportFormatterContext context, PersistentResource<?> resource,
                    OutputStream outputStream) throws IOException {
                outputStream.write(resource.getId().getBytes(StandardCharsets.UTF_8));
            }
        }

        @Bean
        public TableExportFormattersBuilderCustomizer customTableExportFormatter() {
            return builder -> builder.entry("XLSX", new MyTableExportFormatter());
        }

        @Bean
        public ResultTypeFileExtensionMapper resultTypeFileExtensionMapper() {
            return new MyResultTypeFileExtensionMapper();
        }
    }

    @Test
    public void testExportJsonApiCustom() throws InterruptedException {
        //Create Table Export
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("011f99aa-cc41-4c5b-bbb0-d3478aa9d8ac"),
                                        attributes(
                                                attr("query", "/group?fields%5Bgroup%5D=deprecated,commonName,description"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "XLSX")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/json/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/json/tableExport/011f99aa-cc41-4c5b-bbb0-d3478aa9d8ac");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("011f99aa-cc41-4c5b-bbb0-d3478aa9d8ac"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.message", equalTo(null))
                        .body("data.attributes.result.url",
                                equalTo("https://elide.io" + "/export/011f99aa-cc41-4c5b-bbb0-d3478aa9d8ac.xlsx"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ tableExport(ids: [\\\"011f99aa-cc41-4c5b-bbb0-d3478aa9d8ac\\\"]) "
                                + "{ edges { node { id queryType status resultType result "
                                + "{ url httpStatus recordCount } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql")
                        .asString();

                String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"011f99aa-cc41-4c5b-bbb0-d3478aa9d8ac\","
                        + "\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"XLSX\","
                        + "\"result\":{\"url\":\"https://elide.io/export/011f99aa-cc41-4c5b-bbb0-d3478aa9d8ac.xlsx\",\"httpStatus\":200,\"recordCount\":1}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
        }
        String expected = "com.example.repository";
        String response = when()
                .get("/export/011f99aa-cc41-4c5b-bbb0-d3478aa9d8ac.xlsx")
                .asString();
        assertEquals(expected.replaceAll("\r", "").replaceAll("\n", ""),
                response.replaceAll("\r", "").replaceAll("\n", ""));
    }


    @Test
    public void testAsyncApiEndpointOrdered() throws InterruptedException {
        //Create Async Request
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("a85a1372-ebae-4972-ae79-d6f39343b10e"),
                                        attributes(
                                                attr("query", "/group?fields%5Bgroup%5D=commonName,description,deprecated,products"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/json/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/json/asyncQuery/a85a1372-ebae-4972-ae79-d6f39343b10e");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("a85a1372-ebae-4972-ae79-d6f39343b10e"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo("{\"data\":"
                                + "[{\"type\":\"group\",\"id\":\"com.example.repository\",\"attributes\":"
                                + "{\"commonName\":\"Example Repository\",\"description\":\"The code for this project\",\"deprecated\":false},"
                                + "\"relationships\":{\"products\":{\"data\":[]}}}]}"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"a85a1372-ebae-4972-ae79-d6f39343b10e\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ responseBody httpStatus contentLength } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql")
                        .asString();

                String expectedResponse = """
                        {"data":{"asyncQuery":{"edges":[{"node":{"id":"a85a1372-ebae-4972-ae79-d6f39343b10e","queryType":"JSONAPI_V1_0","status":"COMPLETE","result":{"responseBody":"{\\\"data\\\":[{\\\"type\\\":\\\"group\\\",\\\"id\\\":\\\"com.example.repository\\\",\\\"attributes\\\":{\\\"commonName\\\":\\\"Example Repository\\\",\\\"description\\\":\\\"The code for this project\\\",\\\"deprecated\\\":false},\\\"relationships\\\":{\\\"products\\\":{\\\"data\\\":[]}}}]}","httpStatus":200,"contentLength":208}}}]}}}""";
                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
        }
    }

    @Test
    public void testExportJsonApiJsonOrdered() throws InterruptedException {
        //Create Table Export
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("7cf798e8-a924-4f4c-8a45-9ad079668f70"),
                                        attributes(
                                                attr("query", "/group?fields%5Bgroup%5D=deprecated,commonName,description"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "JSON")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/json/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/json/tableExport/7cf798e8-a924-4f4c-8a45-9ad079668f70");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("7cf798e8-a924-4f4c-8a45-9ad079668f70"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.message", equalTo(null))
                        .body("data.attributes.result.url",
                                equalTo("https://elide.io" + "/export/7cf798e8-a924-4f4c-8a45-9ad079668f70.json"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"query\":\"{ tableExport(ids: [\\\"7cf798e8-a924-4f4c-8a45-9ad079668f70\\\"]) "
                                + "{ edges { node { id queryType status resultType result "
                                + "{ url httpStatus recordCount } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql")
                        .asString();

                String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"7cf798e8-a924-4f4c-8a45-9ad079668f70\","
                        + "\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"JSON\","
                        + "\"result\":{\"url\":\"https://elide.io/export/7cf798e8-a924-4f4c-8a45-9ad079668f70.json\",\"httpStatus\":200,\"recordCount\":1}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
        }
        String expected = """
                [
                {"deprecated":false,"commonName":"Example Repository","description":"The code for this project"}
                ]""";
        String response = when()
                .get("/export/7cf798e8-a924-4f4c-8a45-9ad079668f70.json")
                .asString();
        assertEquals(expected.replaceAll("\r", "").replaceAll("\n", ""),
                response.replaceAll("\r", "").replaceAll("\n", ""));
    }

    @Test
    public void testExportJsonApiCsvOrdered() throws InterruptedException {
        //Create Table Export
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("aa8ef302-6236-4c64-a523-6b5a21c62360"),
                                        attributes(
                                                attr("query", "/group?fields%5Bgroup%5D=deprecated,commonName,description"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/json/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/json/tableExport/aa8ef302-6236-4c64-a523-6b5a21c62360");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("aa8ef302-6236-4c64-a523-6b5a21c62360"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.message", equalTo(null))
                        .body("data.attributes.result.url",
                                equalTo("https://elide.io" + "/export/aa8ef302-6236-4c64-a523-6b5a21c62360.csv"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"query\":\"{ tableExport(ids: [\\\"aa8ef302-6236-4c64-a523-6b5a21c62360\\\"]) "
                                + "{ edges { node { id queryType status resultType result "
                                + "{ url httpStatus recordCount } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql")
                        .asString();

                String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"aa8ef302-6236-4c64-a523-6b5a21c62360\","
                        + "\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"CSV\","
                        + "\"result\":{\"url\":\"https://elide.io/export/aa8ef302-6236-4c64-a523-6b5a21c62360.csv\",\"httpStatus\":200,\"recordCount\":1}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
        }
        String expected = """
                "deprecated","commonName","description"
                false, "Example Repository", "The code for this project"
                """;
        String response = when()
                .get("/export/aa8ef302-6236-4c64-a523-6b5a21c62360.csv")
                .asString();
        assertEquals(expected.replaceAll("\r", "").replaceAll("\n", ""),
                response.replaceAll("\r", "").replaceAll("\n", ""));
    }

    @Test
    public void testExportGraphQLJsonOrdered() throws InterruptedException {
        //Create Table Export
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("26a691c1-706b-412a-af8f-2d4861252b08"),
                                        attributes(
                                                attr("query", "{\"query\":\"{ group { edges { node { deprecated commonName description } } } }\",\"variables\":null}"),
                                                attr("queryType", "GRAPHQL_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "JSON")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/json/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/json/tableExport/26a691c1-706b-412a-af8f-2d4861252b08");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("26a691c1-706b-412a-af8f-2d4861252b08"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("GRAPHQL_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.message", equalTo(null))
                        .body("data.attributes.result.url",
                                equalTo("https://elide.io" + "/export/26a691c1-706b-412a-af8f-2d4861252b08.json"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"query\":\"{ tableExport(ids: [\\\"26a691c1-706b-412a-af8f-2d4861252b08\\\"]) "
                                + "{ edges { node { id queryType status resultType result "
                                + "{ url httpStatus recordCount } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql")
                        .asString();

                String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"26a691c1-706b-412a-af8f-2d4861252b08\","
                        + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"JSON\","
                        + "\"result\":{\"url\":\"https://elide.io/export/26a691c1-706b-412a-af8f-2d4861252b08.json\",\"httpStatus\":200,\"recordCount\":1}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
        }
        String expected = """
                [
                {"deprecated":false,"commonName":"Example Repository","description":"The code for this project"}
                ]""";
        String response = when()
                .get("/export/26a691c1-706b-412a-af8f-2d4861252b08.json")
                .asString();
        assertEquals(expected.replaceAll("\r", "").replaceAll("\n", ""),
                response.replaceAll("\r", "").replaceAll("\n", ""));
    }

    @Test
    public void testExportGraphQLCsvOrdered() throws InterruptedException {
        //Create Table Export
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("8349d148-394f-4e03-9d61-81eb8677ae17"),
                                        attributes(
                                                attr("query", "{\"query\":\"{ group { edges { node { deprecated commonName description } } } }\",\"variables\":null}"),
                                                attr("queryType", "GRAPHQL_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/json/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/json/tableExport/8349d148-394f-4e03-9d61-81eb8677ae17");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("8349d148-394f-4e03-9d61-81eb8677ae17"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("GRAPHQL_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.message", equalTo(null))
                        .body("data.attributes.result.url",
                                equalTo("https://elide.io" + "/export/8349d148-394f-4e03-9d61-81eb8677ae17.csv"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"query\":\"{ tableExport(ids: [\\\"8349d148-394f-4e03-9d61-81eb8677ae17\\\"]) "
                                + "{ edges { node { id queryType status resultType result "
                                + "{ url httpStatus recordCount } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql")
                        .asString();

                String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"8349d148-394f-4e03-9d61-81eb8677ae17\","
                        + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"CSV\","
                        + "\"result\":{\"url\":\"https://elide.io/export/8349d148-394f-4e03-9d61-81eb8677ae17.csv\",\"httpStatus\":200,\"recordCount\":1}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
        }
        String expected = """
                "deprecated","commonName","description"
                false, "Example Repository", "The code for this project"
                """;
        String response = when()
                .get("/export/8349d148-394f-4e03-9d61-81eb8677ae17.csv")
                .asString();
        assertEquals(expected.replaceAll("\r", "").replaceAll("\n", ""),
                response.replaceAll("\r", "").replaceAll("\n", ""));
    }

    @Test
    public void testAsyncApiEndpoint() throws InterruptedException {
        //Create Async Request
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                                        attributes(
                                                attr("query", "/group"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/json/asyncQuery")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/json/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo("{\"data\":"
                                + "[{\"type\":\"group\",\"id\":\"com.example.repository\",\"attributes\":"
                                + "{\"commonName\":\"Example Repository\",\"deprecated\":false,\"description\":\"The code for this project\"},"
                                + "\"relationships\":{\"products\":{\"data\":[]}}}]}"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ responseBody httpStatus contentLength } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql")
                        .asString();

                String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\",\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\",\"result\":{\"responseBody\":\"{\\\"data\\\":[{\\\"type\\\":\\\"group\\\",\\\"id\\\":\\\"com.example.repository\\\",\\\"attributes\\\":{\\\"commonName\\\":\\\"Example Repository\\\",\\\"deprecated\\\":false,\\\"description\\\":\\\"The code for this project\\\"},\\\"relationships\\\":{\\\"products\\\":{\\\"data\\\":[]}}}]}\",\"httpStatus\":200,\"contentLength\":208}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
        }
    }

    @Test
    public void testExportDynamicModel() throws InterruptedException {
        //Create Table Export
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9265d"),
                                        attributes(
                                                attr("query", "{\"query\":\"{playerStats(filter:\\\"createdOn>=2020-01-01;createdOn<2020-01-02\\\"){ edges{node{countryCode highScore}}}}\",\"variables\":null}"),
                                                attr("queryType", "GRAPHQL_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/json/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/json/tableExport/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9265d"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("GRAPHQL_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.message", equalTo(null))
                        .body("data.attributes.result.url",
                                equalTo("https://elide.io" + "/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d.csv"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"query\":\"{ tableExport(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9265d\\\"]) "
                                + "{ edges { node { id queryType status resultType result "
                                + "{ url httpStatus recordCount } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql")
                        .asString();

                String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9265d\","
                        + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"CSV\","
                        + "\"result\":{\"url\":\"https://elide.io/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d.csv\",\"httpStatus\":200,\"recordCount\":1}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
        }
        when()
                .get("/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9265d.csv")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testExportStaticModel() throws InterruptedException {
        //Create Table Export
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .body(
                        data(
                                resource(
                                        type("tableExport"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9264d"),
                                        attributes(
                                                attr("query", "{\"query\":\"{ stats { edges { node { dimension measure } } } }\",\"variables\":null}"),
                                                attr("queryType", "GRAPHQL_V1_0"),
                                                attr("status", "QUEUED"),
                                                attr("asyncAfterSeconds", "10"),
                                                attr("resultType", "CSV")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/json/tableExport")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/json/tableExport/ba31ca4e-ed8f-4be0-a0f3-12088fa9264d");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

             //If Async Query is created and completed then validate results
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9264d"))
                        .body("data.type", equalTo("tableExport"))
                        .body("data.attributes.queryType", equalTo("GRAPHQL_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.message", equalTo(null))
                        .body("data.attributes.result.url",
                                equalTo("https://elide.io" + "/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9264d.csv"));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"query\":\"{ tableExport(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9264d\\\"]) "
                                + "{ edges { node { id queryType status resultType result "
                                + "{ url httpStatus recordCount } } } } }\","
                                + "\"variables\":null }")
                        .post("/graphql")
                        .asString();

                String expectedResponse = "{\"data\":{\"tableExport\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9264d\","
                        + "\"queryType\":\"GRAPHQL_V1_0\",\"status\":\"COMPLETE\",\"resultType\":\"CSV\","
                        + "\"result\":{\"url\":\"https://elide.io/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9264d.csv\",\"httpStatus\":200,\"recordCount\":2}}}]}}}";

                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
        }
        when()
                .get("/export/ba31ca4e-ed8f-4be0-a0f3-12088fa9264d.csv")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void exportControllerTest() {
        when()
                .get("/export/asyncQueryId")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void postExportControllerTest() {
        when()
                .post("/export/asyncQueryId")
                .then()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
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
                        "stats", "tableExport", "namespace", "tableSource", "maintainer", "book", "publisher", "person"));
    }
}
