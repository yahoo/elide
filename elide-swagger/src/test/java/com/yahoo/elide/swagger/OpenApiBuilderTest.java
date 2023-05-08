/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.EntityFieldType;
import com.yahoo.elide.core.type.EntityMethodType;
import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.core.type.Method;
import com.yahoo.elide.core.type.Package;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.converters.EpochToDateConverter;
import com.yahoo.elide.core.utils.coerce.converters.TimeZoneSerde;
import com.yahoo.elide.swagger.models.media.Data;
import com.yahoo.elide.swagger.models.media.Datum;
import com.yahoo.elide.swagger.models.media.Relationship;

import example.models.Author;
import example.models.Book;
import example.models.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenApiBuilderTest {
    EntityDictionary dictionary;
    OpenAPI openApi;

    @BeforeAll
    public void setup() {
        dictionary = EntityDictionary.builder().serdeLookup(clazz -> {
            if (TimeZone.class.equals(clazz)) {
                return new TimeZoneSerde();
            } else if (Date.class.equals(clazz)) {
                return new EpochToDateConverter<>(Date.class);
            }
            return null;
        }).build();

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        Info info = new Info().title("Test Service").version(NO_VERSION);

        OpenApiBuilder builder = new OpenApiBuilder(dictionary).apiVersion(info.getVersion());
        openApi = builder.build().info(info);
    }

    @Test
    void testDynamicType() {
        EntityDictionary dynamicDictionary = EntityDictionary.builder().build();

        dynamicDictionary.bindEntity(new DynamicType());
        Info info = new Info().title("Test Service").version(NO_VERSION);

        OpenApiBuilder builder = new OpenApiBuilder(dynamicDictionary).apiVersion(info.getVersion());
        OpenAPI dynamicOpenApi = builder.build();
        assertEquals(2, dynamicOpenApi.getPaths().size());
    }

    @Test
    void testPathGeneration() throws Exception {
        assertTrue(openApi.getPaths().containsKey("/publisher"));
        assertTrue(openApi.getPaths().containsKey("/publisher/{publisherId}"));

        assertTrue(openApi.getPaths().containsKey("/publisher/{publisherId}/books"));
        assertTrue(openApi.getPaths().containsKey("/publisher/{publisherId}/books/{bookId}"));
        assertTrue(openApi.getPaths().containsKey("/publisher/{publisherId}/relationships/books"));

        assertTrue(openApi.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors"));
        assertTrue(openApi.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors/{authorId}"));
        assertTrue(openApi.getPaths().containsKey("/publisher/{publisherId}/relationships/exclusiveAuthors"));

        assertTrue(openApi.getPaths().containsKey("/book"));
        assertTrue(openApi.getPaths().containsKey("/book/{bookId}"));

        assertTrue(openApi.getPaths().containsKey("/book/{bookId}/authors"));
        assertTrue(openApi.getPaths().containsKey("/book/{bookId}/authors/{authorId}"));
        assertTrue(openApi.getPaths().containsKey("/book/{bookId}/relationships/authors"));

        assertTrue(openApi.getPaths().containsKey("/book/{bookId}/publisher"));
        assertTrue(openApi.getPaths().containsKey("/book/{bookId}/publisher/{publisherId}"));
        assertTrue(openApi.getPaths().containsKey("/book/{bookId}/relationships/publisher"));

        assertEquals(16, openApi.getPaths().size());
    }

    @Test
    void testOperationGeneration() throws Exception {
        /* For each path, ensure the correct operations exist */
        openApi.getPaths().forEach((url, path) -> {

            /* All paths should have a GET */
            assertNotNull(path.getGet());

            if (url.contains("relationship")) { //Relationship URL

                /* The relationship is a one to one (so there is no DELETE op */
                if ("/book/{bookId}/relationships/publisher".equals(url)) {
                    assertNull(path.getDelete());
                    assertNull(path.getPost());
                } else {
                    assertNotNull(path.getDelete());
                    assertNotNull(path.getPost());
                }
                assertNotNull(path.getPatch());
            } else if (url.endsWith("Id}")) { //Instance URL
                assertNotNull(path.getDelete());
                assertNotNull(path.getPatch());
                assertNull(path.getPost());
            } else { //Collection URL
                assertNull(path.getDelete());
                assertNull(path.getPatch());
                assertNotNull(path.getPost());
            }
        });
    }

    @Test
    void testPathParams() throws Exception {
        PathItem path = openApi.getPaths().get("/book/{bookId}/authors/{authorId}");
        assertEquals(2,
                path.getParameters().stream()
                .filter((param) -> param.getIn().equals("path"))
                .count());

        Parameter bookId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("bookId"))
                .findFirst()
                .get();

        assertEquals("path", bookId.getIn());
        assertTrue(bookId.getRequired());

        Parameter authorId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("authorId"))
                .findFirst()
                .get();

        assertEquals("path", authorId.getIn());
        assertTrue(authorId.getRequired());

        path = openApi.getPaths().get("/book/{bookId}/authors");
        assertEquals(1,
                path.getParameters().stream()
                .filter((param) -> param.getIn().equals("path"))
                .count());

        bookId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("bookId"))
                .findFirst()
                .get();

        assertEquals("path", bookId.getIn());
        assertTrue(bookId.getRequired());

        path = openApi.getPaths().get("/book/{bookId}/relationships/authors");
        assertEquals(1,
                path.getParameters().stream()
                .filter((param) -> param.getIn().equals("path"))
                .count());

        bookId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("bookId"))
                .findFirst()
                .get();

        assertEquals("path", bookId.getIn());
        assertTrue(bookId.getRequired());
    }

    @Test
    void testOperationRequestBodies() throws Exception {
        /* These take a datum pointing to a resource */
        Operation[] resourceOps = {
                openApi.getPaths().get("/book").getPost(),
                openApi.getPaths().get("/book/{bookId}").getPatch(),
        };

        for (Operation op : resourceOps) {
            assertNotNull(op.getRequestBody());
            verifyDatum(op.getRequestBody().getContent(), "book", true);
        }

        /* These don't take any params */
        Operation[] noParamOps = {
                openApi.getPaths().get("/book").getGet(),
                openApi.getPaths().get("/book/{bookId}").getDelete(),
                openApi.getPaths().get("/book/{bookId}").getGet(),
        };

        for (Operation op : noParamOps) {
            assertNull(op.getRequestBody());
        }

        /* These take a 'data' of relationships */
        Operation[] relationshipOps = {
                openApi.getPaths().get("/book/{bookId}/relationships/authors").getPatch(),
                openApi.getPaths().get("/book/{bookId}/relationships/authors").getDelete(),
                openApi.getPaths().get("/book/{bookId}/relationships/authors").getPost(),
        };

        for (Operation op : relationshipOps) {
            assertNotNull(op.getRequestBody());
            verifyDataRelationship(op.getRequestBody().getContent(), "author");
        }
    }

    @Test
    void testOperationSuccessResponseBodies() throws Exception {
        ApiResponse response = openApi.getPaths().get("/book").getGet().getResponses().get("200");
        verifyData(response.getContent(), "book");

        response = openApi.getPaths().get("/book").getPost().getResponses().get("201");
        verifyDatum(response.getContent(), "book", false);

        response = openApi.getPaths().get("/book/{bookId}").getGet().getResponses().get("200");
        verifyDatum(response.getContent(), "book", true);

        response = openApi.getPaths().get("/book/{bookId}").getPatch().getResponses().get("204");
        assertNull(response.getContent());

        response = openApi.getPaths().get("/book/{bookId}").getDelete().getResponses().get("204");
        assertNull(response.getContent());

        response = openApi.getPaths().get("/book/{bookId}/relationships/authors").getGet().getResponses().get("200");
        verifyDataRelationship(response.getContent(), "author");

        response = openApi.getPaths().get("/book/{bookId}/relationships/authors").getPost().getResponses().get("201");
        verifyDataRelationship(response.getContent(), "author");

        response = openApi.getPaths().get("/book/{bookId}/relationships/authors").getPatch().getResponses().get("204");
        assertNull(response.getContent());

        response = openApi.getPaths().get("/book/{bookId}/relationships/authors").getDelete().getResponses().get("204");
        assertNull(response.getContent());
    }

    @Test
    void testOperationSuccessResponseCodes() throws Exception {
        /* For each path, ensure the correct operations exist */
        openApi.getPaths().forEach((url, path) -> {

            Operation getOperation = path.getGet();
            assertTrue(getOperation.getResponses().containsKey("200"));

            if (url.contains("relationship")) { //Relationship URL

                if (path.getDelete() != null) {
                    Operation deleteOperation = path.getDelete();
                    assertTrue(deleteOperation.getResponses().containsKey("204"));
                }

                if (path.getPost() != null) {
                    Operation postOperation = path.getPost();
                    assertTrue(postOperation.getResponses().containsKey("201"));
                }

                Operation patchOperation = path.getPatch();
                assertTrue(patchOperation.getResponses().containsKey("204"));
            } else if (url.endsWith("Id}")) { //Instance URL
                Operation deleteOperation = path.getDelete();
                assertTrue(deleteOperation.getResponses().containsKey("204"));

                Operation patchOperation = path.getPatch();
                assertTrue(patchOperation.getResponses().containsKey("204"));
            } else { //Collection URL
                Operation postOperation = path.getPost();
                assertTrue(postOperation.getResponses().containsKey("201"));
            }
        });
    }

    @Test
    void testFilterParam() throws Exception {

        /* Test root filters */
        List<Parameter> params = openApi.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long filterParams = paramNames.stream().filter((name) -> name.startsWith("filter")).count();
        assertEquals(24, filterParams);

        assertTrue(paramNames.contains("filter"));
        assertTrue(paramNames.contains("filter[book]"));
        assertTrue(paramNames.contains("filter[book.title][in]"));
        assertTrue(paramNames.contains("filter[book.title][not]"));
        assertTrue(paramNames.contains("filter[book.title][prefix]"));
        assertTrue(paramNames.contains("filter[book.title][infix]"));
        assertTrue(paramNames.contains("filter[book.title][postfix]"));
        assertTrue(paramNames.contains("filter[book.title][isnull]"));
        assertTrue(paramNames.contains("filter[book.title][notnull]"));
        assertTrue(paramNames.contains("filter[book.title][lt]"));
        assertTrue(paramNames.contains("filter[book.title][gt]"));
        assertTrue(paramNames.contains("filter[book.title][le]"));
        assertTrue(paramNames.contains("filter[book.title][ge]"));


        /* Test relationships filters */
        params = openApi.getPaths().get("/book/{bookId}/relationships/authors").getGet().getParameters();
        paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        filterParams = paramNames.stream().filter((name) -> name.startsWith("filter")).count();
        assertEquals(23, filterParams);

        assertTrue(paramNames.contains("filter[author]"));
        assertTrue(paramNames.contains("filter[author.name][in]"));
        assertTrue(paramNames.contains("filter[author.name][not]"));
        assertTrue(paramNames.contains("filter[author.name][prefix]"));
        assertTrue(paramNames.contains("filter[author.name][infix]"));
        assertTrue(paramNames.contains("filter[author.name][postfix]"));
        assertTrue(paramNames.contains("filter[author.name][isnull]"));
        assertTrue(paramNames.contains("filter[author.name][notnull]"));
        assertTrue(paramNames.contains("filter[author.name][lt]"));
        assertTrue(paramNames.contains("filter[author.name][gt]"));
        assertTrue(paramNames.contains("filter[author.name][le]"));
        assertTrue(paramNames.contains("filter[author.name][ge]"));
    }

    @Test
    void testPageParam() throws Exception {
        /* Tests root collection */
        List<Parameter> params = openApi.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long pageParams = paramNames.stream().filter((name) -> name.startsWith("page")).count();
        assertEquals(5, pageParams);

        assertTrue(paramNames.contains("page[number]"));
        assertTrue(paramNames.contains("page[size]"));
        assertTrue(paramNames.contains("page[offset]"));
        assertTrue(paramNames.contains("page[limit]"));
        assertTrue(paramNames.contains("page[totals]"));

        /* Tests relationship collection */
        params = openApi.getPaths().get("/book/{bookId}/relationships/authors").getGet().getParameters();

        paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        pageParams = paramNames.stream().filter((name) -> name.startsWith("page")).count();
        assertEquals(5, pageParams);

        assertTrue(paramNames.contains("page[number]"));
        assertTrue(paramNames.contains("page[size]"));
        assertTrue(paramNames.contains("page[offset]"));
        assertTrue(paramNames.contains("page[limit]"));
        assertTrue(paramNames.contains("page[totals]"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSortParam() throws Exception {
        List<Parameter> params = openApi.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long sortParams = paramNames.stream().filter((name) -> name.startsWith("sort")).count();
        assertEquals(1, sortParams);
        assertTrue(paramNames.contains("sort"));

        QueryParameter sortParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("sort"))
                .findFirst()
                .get();

        assertEquals("query", sortParam.getIn());

        List<String> sortValues = Arrays.asList("id", "-id", "title", "-title");
        assertTrue(sortParam.getSchema().getItems().getEnum().containsAll(sortValues));
//        assertEquals("csv", sortParam.getCollectionFormat());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testIncludeParam() throws Exception {
        List<Parameter> params = openApi.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long includeParams = paramNames.stream().filter((name) -> name.startsWith("include")).count();
        assertEquals(1, includeParams);
        assertTrue(paramNames.contains("include"));

        QueryParameter includeParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("include"))
                .findFirst()
                .get();

        assertEquals("query", includeParam.getIn());

        List<String> includeValues = Arrays.asList("authors", "publisher");
        assertTrue(includeParam.getSchema().getItems().getEnum().containsAll(includeValues));
//        assertEquals("csv", includeParam.getCollectionFormat());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSparseFieldsParam() throws Exception {
        List<Parameter> params = openApi.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long fieldParams = paramNames.stream().filter((name) -> name.startsWith("fields")).count();
        assertEquals(1, fieldParams);
        assertTrue(paramNames.contains("fields[book]"));

        QueryParameter fieldParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("fields[book]"))
                .findFirst()
                .get();

        assertEquals("query", fieldParam.getIn());

        List<String> filterValues = Arrays.asList("title", "authors", "publisher");
        assertTrue(fieldParam.getSchema().getItems().getEnum().containsAll(filterValues));
//        assertEquals("csv", fieldParam.getCollectionFormat());
    }

    @Test
    void testTagGeneration() throws Exception {

        /* Check for the global tag definitions */
        assertEquals(3, openApi.getTags().size());

        String bookTag = openApi.getTags().stream()
                .filter((tag) -> tag.getName().equals("book"))
                .findFirst().get().getName();

        assertNotNull(bookTag);

        Tag publisherTag = openApi.getTags().stream()
                .filter((tag) -> tag.getName().equals("publisher"))
                .findFirst().get();

        assertNotNull(publisherTag);
        assertEquals("Publisher information.", publisherTag.getDescription());

        /* For each operation, ensure its tagged with the root collection name */
        openApi.getPaths().forEach((url, path) -> {
            if (url.endsWith("relationships/books")) {
                path.getGet().getTags().contains(bookTag);
                path.getPost().getTags().contains(bookTag);
                path.getDelete().getTags().contains(bookTag);
                path.getPatch().getTags().contains(bookTag);
            } else if (url.endsWith("/books")) {
                path.getGet().getTags().contains(bookTag);
                path.getPost().getTags().contains(bookTag);
            } else if (url.endsWith("{bookId}")) {
                path.getGet().getTags().contains(bookTag);
                path.getPatch().getTags().contains(bookTag);
                path.getDelete().getTags().contains(bookTag);
            } else if (url.endsWith("relationships/publisher")) {
                path.getGet().getTags().contains(publisherTag);
                path.getPatch().getTags().contains(publisherTag);
            } else if (url.endsWith("/publisher")) {
                path.getGet().getTags().contains(publisherTag);
                path.getPost().getTags().contains(publisherTag);
            } else if (url.endsWith("{publisherId}")) {
                path.getGet().getTags().contains(publisherTag);
                path.getPatch().getTags().contains(publisherTag);
                path.getDelete().getTags().contains(publisherTag);
            }
        });
    }

    @Test
    void testGlobalErrorResponses() throws Exception {
        Info info = new Info()
                .title("Test Service")
                .version(NO_VERSION);

        OpenApiBuilder builder = new OpenApiBuilder(dictionary).apiVersion(info.getVersion());

        Map<String, ApiResponse> responses = new HashMap<>();

        responses.put("401", OpenApiBuilder.UNAUTHORIZED_RESPONSE);
        responses.put("403", OpenApiBuilder.FORBIDDEN_RESPONSE);
        responses.put("404", OpenApiBuilder.NOT_FOUND_RESPONSE);
        responses.put("408", OpenApiBuilder.REQUEST_TIMEOUT_RESPONSE);
        responses.put("429", OpenApiBuilder.REQUEST_TIMEOUT_RESPONSE);

        responses.forEach(builder::globalResponse);

        OpenAPI openApi = builder.build().info(info);

        Operation [] ops = {
                openApi.getPaths().get("/book/{bookId}").getGet(),
                openApi.getPaths().get("/book/{bookId}").getDelete(),
                openApi.getPaths().get("/publisher/{publisherId}/relationships/exclusiveAuthors").getPatch(),
                openApi.getPaths().get("/publisher").getPost(),
        };

        for (Operation op : ops) {
            responses.forEach(
                    (code, response) -> {
                        String key = String.valueOf(code);
                        assertEquals(response, op.getResponses().get(key));
                    }
            );
        }
    }

    @Test
    void testSortParameter() {
        @Entity
        @Include
        class NothingToSort {
            @Id
            long name;
        }
        EntityDictionary entityDictionary = EntityDictionary.builder().build();

        entityDictionary.bindEntity(NothingToSort.class);
        Info info = new Info().title("Test Service").version(NO_VERSION);

        OpenApiBuilder builder = new OpenApiBuilder(entityDictionary).apiVersion(info.getVersion());
        OpenAPI testOpenApi = builder.build().info(info);

        List<Parameter> params = testOpenApi.getPaths().get("/nothingToSort").getGet().getParameters();

        QueryParameter sortParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("sort"))
                .findFirst()
                .get();

        assertEquals("query", sortParam.getIn());

        List<String> sortValues = Arrays.asList("id", "-id");
        assertEquals(sortValues, ((StringSchema) sortParam.getSchema().getItems()).getEnum());
    }

    @Test
    void testAllFilterParameters() throws Exception {
        Info info = new Info()
                .title("Test Service");

        OpenApiBuilder builder = new OpenApiBuilder(dictionary).apiVersion(info.getVersion());
        OpenAPI openApi = builder.build().info(info);

        Operation op = openApi.getPaths().get("/book").getGet();

        List<String> paramNames = op.getParameters().stream()
                .filter(param -> param.getName().startsWith("filter"))
                .map(Parameter::getName)
                .sorted()
                .collect(Collectors.toList());

        List<String> expectedNames = Arrays.asList("filter", "filter[book.title][ge]", "filter[book.title][gt]",
                "filter[book.title][in]", "filter[book.title][infix]", "filter[book.title][isnull]",
                "filter[book.title][le]", "filter[book.title][lt]", "filter[book.title][not]",
                "filter[book.title][notnull]", "filter[book.title][postfix]", "filter[book.title][prefix]",
                "filter[book.year][ge]", "filter[book.year][gt]", "filter[book.year][in]", "filter[book.year][infix]",
                "filter[book.year][isnull]", "filter[book.year][le]", "filter[book.year][lt]", "filter[book.year][not]",
                "filter[book.year][notnull]", "filter[book.year][postfix]", "filter[book.year][prefix]",
                "filter[book]");

        assertEquals(expectedNames, paramNames);
    }

    @Test
    void testRsqlOnlyFilterParameters() throws Exception {
        Info info = new Info()
                .title("Test Service");

        OpenApiBuilder builder = new OpenApiBuilder(dictionary).apiVersion(info.getVersion());
        builder = builder.supportLegacyFilterDialect(false);
        OpenAPI openApi = builder.build().info(info);

        Operation op = openApi.getPaths().get("/book").getGet();

        List<String> paramNames = op.getParameters().stream()
                .filter(param -> param.getName().startsWith("filter"))
                .map(Parameter::getName)
                .sorted()
                .collect(Collectors.toList());

        List<String> expectedNames = Arrays.asList("filter", "filter[book]");

        assertEquals(expectedNames, paramNames);
    }

    @Test
    void testLegacyOnlyFilterParameters() throws Exception {
        Info info = new Info()
                .title("Test Service");

        OpenApiBuilder builder = new OpenApiBuilder(dictionary).apiVersion(info.getVersion());
        builder = builder.supportRSQLFilterDialect(false);
        OpenAPI openApi = builder.build().info(info);

        Operation op = openApi.getPaths().get("/book").getGet();

        List<String> paramNames = op.getParameters().stream()
                .filter(param -> param.getName().startsWith("filter"))
                .map(Parameter::getName)
                .sorted()
                .collect(Collectors.toList());

        List<String> expectedNames = Arrays.asList("filter[book.title][ge]", "filter[book.title][gt]",
                "filter[book.title][in]", "filter[book.title][infix]", "filter[book.title][isnull]",
                "filter[book.title][le]", "filter[book.title][lt]", "filter[book.title][not]",
                "filter[book.title][notnull]", "filter[book.title][postfix]", "filter[book.title][prefix]",
                "filter[book.year][ge]", "filter[book.year][gt]", "filter[book.year][in]", "filter[book.year][infix]",
                "filter[book.year][isnull]", "filter[book.year][le]", "filter[book.year][lt]", "filter[book.year][not]",
                "filter[book.year][notnull]", "filter[book.year][postfix]", "filter[book.year][prefix]");

        assertEquals(expectedNames, paramNames);
    }

    @Test
    void testSchemaTitleFromFriendlyName() {
        OpenAPI openApi = new OpenApiBuilder(dictionary).build();
        Schema<?> publisher = openApi.getComponents().getSchemas().get("publisher");
        assertEquals("Publisher Title", publisher.getTitle());
    }

    @Test
    void testSchemaTitleShouldOverrideFriendlyName() {
        OpenAPI openApi = new OpenApiBuilder(dictionary).build();
        Schema<?> book = openApi.getComponents().getSchemas().get("book");
        assertEquals("Override Include Title", book.getTitle());
    }

    @Test
    void testTimeZoneSerde() {
        OpenAPI openApi = new OpenApiBuilder(dictionary).build();
        Schema<?> publisher = openApi.getComponents().getSchemas().get("publisher");
        Schema<?> attributes = publisher.getProperties().get("attributes");
        Schema<?> timeZone = attributes.getProperties().get("timeZone");
        assertEquals("string", timeZone.getType());
        assertEquals("Time Zone", timeZone.getDescription());
    }

    @Test
    void testEpochSerde() {
        OpenAPI openApi = new OpenApiBuilder(dictionary).build();
        Schema<?> book = openApi.getComponents().getSchemas().get("book");
        Schema<?> attributes = book.getProperties().get("attributes");
        Schema<?> publishedOn = attributes.getProperties().get("publishedOn");
        assertEquals("integer", publishedOn.getType());
    }

    @Test
    void testRequiredAttribute() {
        OpenAPI openApi = new OpenApiBuilder(dictionary).build();
        Schema<?> book = openApi.getComponents().getSchemas().get("book");
        Schema<?> attributes = book.getProperties().get("attributes");
        assertTrue(attributes.getRequired().contains("title"));
    }

    @Test
    void testRequiredRelationship() {
        OpenAPI openApi = new OpenApiBuilder(dictionary).build();
        Schema<?> book = openApi.getComponents().getSchemas().get("book");
        Schema<?> relationships = book.getProperties().get("relationships");
        assertTrue(relationships.getRequired().contains("authors"));
    }

    /**
     * Verifies that the given property is of type 'Data' containing a reference to the given model.
     * @param content The content to check
     * @param refTypeName The model name
     */
    private void verifyData(Content content, String refTypeName) {
        verifyData(content.get(JSONAPI_CONTENT_TYPE).getSchema(), refTypeName);
    }

    /**
     * Verifies that the given property is of type 'Data' containing a reference to the given model.
     * @param schema The property to check
     * @param refTypeName The model name
     */
    private void verifyData(Schema<?> schema, String refTypeName) {
        assertTrue((schema instanceof Data));

        ArraySchema data = (ArraySchema) ((Data) schema).getProperties().get("data");

        Schema<?> ref = data.getItems();

        assertEquals("#/components/schemas/" + refTypeName, ref.get$ref());
    }

    /**
     * Verifies that the given property is of type 'Datum' containing a reference to the given model.
     * @param content The content to check
     * @param refTypeName The model name
     * @param included Whether or not the datum should have an 'included' section.
     */
    private void verifyDatum(Content content, String refTypeName, boolean included) {
        verifyDatum(content.get(JSONAPI_CONTENT_TYPE).getSchema(), refTypeName, included);
    }

    /**
     * Verifies that the given property is of type 'Datum' containing a reference to the given model.
     * @param schema The property to check
     * @param refTypeName The model name
     * @param included Whether or not the datum should have an 'included' section.
     */
    private void verifyDatum(Schema<?> schema, String refTypeName, boolean included) {
        assertTrue((schema instanceof Datum));

        Schema<?> ref = ((Datum) schema).getProperties().get("data");

        assertEquals("#/components/schemas/" + refTypeName, ref.get$ref());

        if (included) {
            assertNotNull(((Datum) schema).getProperties().get("included"));
        }
    }

    /**
     * Verifies that the given property is of type 'Data' containing a 'Relationship' with the
     * correct type field.
     * @param content The content to check
     * @param refTypeName The type field to match against
     */
    private void verifyDataRelationship(Content content, String refTypeName) {
        verifyDataRelationship(content.get(JSONAPI_CONTENT_TYPE).getSchema(), refTypeName);
    }

    /**
     * Verifies that the given property is of type 'Data' containing a 'Relationship' with the
     * correct type field.
     * @param schema The property to check
     * @param refTypeName The type field to match against
     */
    private void verifyDataRelationship(Schema<?> schema, String refTypeName) {
        assertTrue((schema instanceof Data));

        ArraySchema data = (ArraySchema) schema.getProperties().get("data");

        Relationship relation = (Relationship) data.getItems();
        StringSchema type = (StringSchema) relation.getProperties().get("type");
        assertTrue(type.getEnum().contains(refTypeName));
    }

    public static class DynamicType implements Type<Object> {

        @Include
        public static class Entity {
        }

        private static final long serialVersionUID = 1L;

        @Override
        public String getCanonicalName() {
            return null;
        }

        @Override
        public String getSimpleName() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Method getMethod(String name, Type<?>... parameterTypes) throws NoSuchMethodException {
            return null;
        }

        @Override
        public Type<?> getSuperclass() {
            return null;
        }

        @Override
        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
            return Entity.class.getAnnotationsByType(annotationClass);
        }

        @Override
        public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
            return Entity.class.getDeclaredAnnotation(annotationClass);
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
            return null;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }

        @Override
        public boolean isAssignableFrom(Type<?> cls) {
            return false;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public Package getPackage() {
            return null;
        }

        @Override
        public Method[] getMethods() {
            return Arrays.stream(Entity.class.getMethods()).map(EntityMethodType::new).toArray(Method[]::new);
        }

        @Override
        public Method[] getDeclaredMethods() {
            return Arrays.stream(Entity.class.getDeclaredMethods()).map(EntityMethodType::new).toArray(Method[]::new);
        }

        @Override
        public Field[] getFields() {
            return Arrays.stream(Entity.class.getFields()).map(EntityFieldType::new).toArray(Field[]::new);
        }

        @Override
        public Field[] getDeclaredFields() {
            return Arrays.stream(Entity.class.getDeclaredFields()).map(EntityFieldType::new).toArray(Field[]::new);
        }

        @Override
        public Field getDeclaredField(String name) throws NoSuchFieldException {
            return null;
        }

        @Override
        public Method[] getConstructors() {
            return Arrays.stream(Entity.class.getConstructors()).map(EntityMethodType::new).toArray(Method[]::new);
        }

        @Override
        public boolean hasSuperType() {
            return false;
        }

        @Override
        public Object newInstance() throws InstantiationException, IllegalAccessException {
            return null;
        }

        @Override
        public boolean isEnum() {
            return false;
        }

        @Override
        public Object[] getEnumConstants() {
            return null;
        }

        @Override
        public Optional<Class<Object>> getUnderlyingClass() {
            return Optional.empty();
        }
    }
}
