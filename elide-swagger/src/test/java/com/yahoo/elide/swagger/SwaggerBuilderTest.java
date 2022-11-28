/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.swagger.model.Resource;
import com.yahoo.elide.swagger.property.Data;
import com.yahoo.elide.swagger.property.Datum;
import com.yahoo.elide.swagger.property.Relationship;
import example.models.Author;
import example.models.Book;
import example.models.Publisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SwaggerBuilderTest {
    EntityDictionary dictionary;
    Swagger swagger;

    @BeforeAll
    public void setup() {
        dictionary = EntityDictionary.builder().build();

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        Info info = new Info().title("Test Service").version(NO_VERSION);

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);
        swagger = builder.build();
    }

    @Test
    public void testPathGeneration() throws Exception {
        assertTrue(swagger.getPaths().containsKey("/publisher"));
        assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}"));

        assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/books"));
        assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/books/{bookId}"));
        assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/relationships/books"));

        assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors"));
        assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors/{authorId}"));
        assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/relationships/exclusiveAuthors"));

        assertTrue(swagger.getPaths().containsKey("/book"));
        assertTrue(swagger.getPaths().containsKey("/book/{bookId}"));

        assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors"));
        assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors/{authorId}"));
        assertTrue(swagger.getPaths().containsKey("/book/{bookId}/relationships/authors"));

        assertTrue(swagger.getPaths().containsKey("/book/{bookId}/publisher"));
        assertTrue(swagger.getPaths().containsKey("/book/{bookId}/publisher/{publisherId}"));
        assertTrue(swagger.getPaths().containsKey("/book/{bookId}/relationships/publisher"));

        assertEquals(16, swagger.getPaths().size());
    }

    @Test
    public void testOperationGeneration() throws Exception {
        /* For each path, ensure the correct operations exist */
        swagger.getPaths().forEach((url, path) -> {

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
    public void testPathParams() throws Exception {
        Path path = swagger.getPaths().get("/book/{bookId}/authors/{authorId}");
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

        path = swagger.getPaths().get("/book/{bookId}/authors");
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

        path = swagger.getPaths().get("/book/{bookId}/relationships/authors");
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
    public void testOperationRequestBodies() throws Exception {
        /* These take a datum pointing to a resource */
        Operation[] resourceOps = {
                swagger.getPaths().get("/book").getPost(),
                swagger.getPaths().get("/book/{bookId}").getPatch(),
        };

        for (Operation op : resourceOps) {
            BodyParameter bodyParam = (BodyParameter) op.getParameters().stream()
                    .filter((param) -> param.getIn().equals("body"))
                    .findFirst()
                    .get();

            assertNotNull(bodyParam);
            verifyDatum(bodyParam.getSchema(), "book");
        }

        /* These don't take any params */
        Operation[] noParamOps = {
                swagger.getPaths().get("/book").getGet(),
                swagger.getPaths().get("/book/{bookId}").getDelete(),
                swagger.getPaths().get("/book/{bookId}").getGet(),
        };

        for (Operation op : noParamOps) {
             Optional<Parameter> bodyParam = op.getParameters().stream()
                    .filter((param) -> param.getIn().equals("body"))
                    .findFirst();

            assertFalse(bodyParam.isPresent());
        }

        /* These take a 'data' of relationships */
        Operation[] relationshipOps = {
                swagger.getPaths().get("/book/{bookId}/relationships/authors").getPatch(),
                swagger.getPaths().get("/book/{bookId}/relationships/authors").getDelete(),
                swagger.getPaths().get("/book/{bookId}/relationships/authors").getPost(),
        };

        for (Operation op : relationshipOps) {
            BodyParameter bodyParam = (BodyParameter) op.getParameters().stream()
                    .filter((param) -> param.getIn().equals("body"))
                    .findFirst()
                    .get();
            assertNotNull(bodyParam);
            verifyDataRelationship(bodyParam.getSchema(), "author");
        }
    }

    @Test
    public void testOperationSuccessResponseBodies() throws Exception {
        Response response = swagger.getPaths().get("/book").getGet().getResponses().get("200");
        verifyData(response.getSchema(), "book");

        response = swagger.getPaths().get("/book").getPost().getResponses().get("201");
        verifyDatum(response.getSchema(), "book", false);

        response = swagger.getPaths().get("/book/{bookId}").getGet().getResponses().get("200");
        verifyDatum(response.getSchema(), "book", true);

        response = swagger.getPaths().get("/book/{bookId}").getPatch().getResponses().get("204");
        assertNull(response.getSchema());

        response = swagger.getPaths().get("/book/{bookId}").getDelete().getResponses().get("204");
        assertNull(response.getSchema());

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getGet().getResponses().get("200");
        verifyDataRelationship(response.getSchema(), "author");

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getPost().getResponses().get("201");
        verifyDataRelationship(response.getSchema(), "author");

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getPatch().getResponses().get("204");
        assertNull(response.getSchema());

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getDelete().getResponses().get("204");
        assertNull(response.getSchema());
    }

    @Test
    public void testOperationSuccessResponseCodes() throws Exception {
        /* For each path, ensure the correct operations exist */
        swagger.getPaths().forEach((url, path) -> {

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
    public void testFilterParam() throws Exception {

        /* Test root filters */
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

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
        params = swagger.getPaths().get("/book/{bookId}/relationships/authors").getGet().getParameters();
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
    public void testPageParam() throws Exception {
        /* Tests root collection */
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

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
        params = swagger.getPaths().get("/book/{bookId}/relationships/authors").getGet().getParameters();

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

    @Test
    public void testSortParam() throws Exception {
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

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
        assertTrue(((StringProperty) sortParam.getItems()).getEnum().containsAll(sortValues));
        assertEquals("csv", sortParam.getCollectionFormat());
    }

    @Test
    public void testIncludeParam() throws Exception {
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

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
        assertTrue(((StringProperty) includeParam.getItems()).getEnum().containsAll(includeValues));
        assertEquals("csv", includeParam.getCollectionFormat());
    }

    @Test
    public void testSparseFieldsParam() throws Exception {
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

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
        assertTrue(((StringProperty) fieldParam.getItems()).getEnum().containsAll(filterValues));
        assertEquals("csv", fieldParam.getCollectionFormat());
    }

    @Test
    public void testDefinitionGeneration() throws Exception {
        Map<String, Model> definitions = swagger.getDefinitions();

        assertEquals(4, definitions.size());
        assertTrue(definitions.containsKey("book"));
        assertTrue(definitions.containsKey("author"));
        assertTrue(definitions.containsKey("publisher"));
        assertTrue(definitions.containsKey("Address"));

        Model bookModel = definitions.get("book");
        assertTrue(bookModel instanceof Resource);

        assertEquals("A book", bookModel.getDescription());

        ObjectProperty attributeProps = (ObjectProperty) bookModel.getProperties().get("attributes");
        assertTrue(attributeProps.getProperties().containsKey("title"));

        ObjectProperty relationProps = (ObjectProperty) bookModel.getProperties().get("relationships");
        assertTrue(relationProps.getProperties().containsKey("publisher"));
        assertTrue(relationProps.getProperties().containsKey("authors"));
    }

    @Test
    public void testTagGeneration() throws Exception {

        /* Check for the global tag definitions */
        assertEquals(3, swagger.getTags().size());

        Tag bookTag = swagger.getTags().stream()
                .filter((tag) -> tag.getName().equals("book"))
                .findFirst().get();

        assertNotNull(bookTag);

        Tag publisherTag = swagger.getTags().stream()
                .filter((tag) -> tag.getName().equals("publisher"))
                .findFirst().get();

        assertNotNull(publisherTag);

        /* For each operation, ensure its tagged with the root collection name */
        swagger.getPaths().forEach((url, path) -> {
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
    public void testGlobalErrorResponses() throws Exception {
        Info info = new Info()
                .title("Test Service")
                .version(NO_VERSION);

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);

        Map<Integer, Response> responses = new HashMap<>();

        responses.put(401, SwaggerBuilder.UNAUTHORIZED_RESPONSE);
        responses.put(403, SwaggerBuilder.FORBIDDEN_RESPONSE);
        responses.put(404, SwaggerBuilder.NOT_FOUND_RESPONSE);
        responses.put(408, SwaggerBuilder.REQUEST_TIMEOUT_RESPONSE);
        responses.put(429, SwaggerBuilder.REQUEST_TIMEOUT_RESPONSE);

        responses.forEach(
                (code, response) -> {
                    builder.withGlobalResponse(code, response);

                }
        );

        Swagger swagger = builder.build();

        Operation [] ops = {
                swagger.getPaths().get("/book/{bookId}").getGet(),
                swagger.getPaths().get("/book/{bookId}").getDelete(),
                swagger.getPaths().get("/publisher/{publisherId}/relationships/exclusiveAuthors").getPatch(),
                swagger.getPaths().get("/publisher").getPost(),
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
    public void testSortParameter() {
        @Entity
        @Include
        class NothingToSort {
            @Id
            long name;
        }
        EntityDictionary entityDictionary = EntityDictionary.builder().build();

        entityDictionary.bindEntity(NothingToSort.class);
        Info info = new Info().title("Test Service").version(NO_VERSION);

        SwaggerBuilder builder = new SwaggerBuilder(entityDictionary, info);
        Swagger testSwagger = builder.build();

        List<Parameter> params = testSwagger.getPaths().get("/nothingToSort").getGet().getParameters();

        QueryParameter sortParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("sort"))
                .findFirst()
                .get();

        assertEquals("query", sortParam.getIn());

        List<String> sortValues = Arrays.asList("id", "-id");
        assertEquals(sortValues, ((StringProperty) sortParam.getItems()).getEnum());
    }

    @Test
    public void testAllFilterParameters() throws Exception {
        Info info = new Info()
                .title("Test Service");

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);
        Swagger swagger = builder.build();

        Operation op = swagger.getPaths().get("/book").getGet();

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
    public void testRsqlOnlyFilterParameters() throws Exception {
        Info info = new Info()
                .title("Test Service");

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);
        builder = builder.withLegacyFilterDialect(false);
        Swagger swagger = builder.build();

        Operation op = swagger.getPaths().get("/book").getGet();

        List<String> paramNames = op.getParameters().stream()
                .filter(param -> param.getName().startsWith("filter"))
                .map(Parameter::getName)
                .sorted()
                .collect(Collectors.toList());

        List<String> expectedNames = Arrays.asList("filter", "filter[book]");

        assertEquals(expectedNames, paramNames);
    }

    @Test
    public void testLegacyOnlyFilterParameters() throws Exception {
        Info info = new Info()
                .title("Test Service");

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);
        builder = builder.withRSQLFilterDialect(false);
        Swagger swagger = builder.build();

        Operation op = swagger.getPaths().get("/book").getGet();

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

    /**
     * Verifies that the given property is of type 'Data' containing a reference to the given model.
     * @param property The property to check
     * @param refTypeName The model name
     */
    private void verifyData(Property property, String refTypeName) {
        assertTrue((property instanceof Data));

        ArrayProperty data = (ArrayProperty) ((Data) property).getProperties().get("data");

        RefProperty ref = (RefProperty) data.getItems();
        assertEquals("#/definitions/" + refTypeName, ref.get$ref());
    }

    /**
     * Verifies that the given model is of type 'Data' containing a reference to the given model name.
     * @param model The model to check
     * @param refTypeName The model name to check
     */
    private void verifyData(Model model, String refTypeName) {
        assertTrue((model instanceof com.yahoo.elide.swagger.model.Data));

        ArrayProperty data = (ArrayProperty) model.getProperties().get("model");

        RefProperty ref = (RefProperty) data.getItems();
        assertEquals("#/definitions/" + refTypeName, ref.get$ref());
    }

    /**
     * Verifies that the given property is of type 'Datum' containing a reference to the given model.
     * @param property The property to check
     * @param refTypeName The model name
     * @param included Whether or not the datum should have an 'included' section.
     */
    private void verifyDatum(Property property, String refTypeName, boolean included) {
        assertTrue((property instanceof Datum));

        RefProperty ref = (RefProperty) ((Datum) property).getProperties().get("data");

        assertEquals("#/definitions/" + refTypeName, ref.get$ref());

        if (included) {
            assertNotNull(((Datum) property).getProperties().get("included"));
        }
    }

    /**
     * Verifies that the given model is of type 'Datum' containing a reference to the given model name.
     * @param model The model to check
     * @param refTypeName The model name to check
     */
    private void verifyDatum(Model model, String refTypeName) {
        assertTrue((model instanceof com.yahoo.elide.swagger.model.Datum));

        RefProperty ref = (RefProperty) model.getProperties().get("data");

        assertEquals("#/definitions/" + refTypeName, ref.get$ref());
    }

    /**
     * Verifies that the given property is of type 'Data' containing a 'Relationship' with the
     * correct type field.
     * @param property The property to check
     * @param refTypeName The type field to match against
     */
    private void verifyDataRelationship(Property property, String refTypeName) {
        assertTrue((property instanceof Data));

        ArrayProperty data = (ArrayProperty) ((Data) property).getProperties().get("data");

        Relationship relation = (Relationship) data.getItems();
        StringProperty type = (StringProperty) relation.getProperties().get("type");
        assertTrue(type.getEnum().contains(refTypeName));
    }

     /**
     * Verifies that the given model is of type 'Data' containing a 'Relationship' with the
     * correct type field.
     * @param model The model to check
     * @param refTypeName The type field to match against
     */
    private void verifyDataRelationship(Model model, String refTypeName) {
        assertTrue((model instanceof com.yahoo.elide.swagger.model.Data));

        ArrayProperty data = (ArrayProperty) model.getProperties().get("data");

        Relationship relation = (Relationship) data.getItems();
        StringProperty type = (StringProperty) relation.getProperties().get("type");
        assertTrue(type.getEnum().contains(refTypeName));
    }
}
