/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.contrib.swagger.model.Resource;
import com.yahoo.elide.contrib.swagger.models.Author;
import com.yahoo.elide.contrib.swagger.models.Book;
import com.yahoo.elide.contrib.swagger.models.Publisher;
import com.yahoo.elide.contrib.swagger.property.Data;
import com.yahoo.elide.contrib.swagger.property.Datum;
import com.yahoo.elide.contrib.swagger.property.Relationship;
import com.yahoo.elide.core.EntityDictionary;

import com.google.common.collect.Maps;

import org.junit.jupiter.api.Assertions;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.Id;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SwaggerBuilderTest {
    EntityDictionary dictionary;
    Swagger swagger;

    @BeforeAll
    public void setup() {
        dictionary = new EntityDictionary(Maps.newHashMap());

        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Publisher.class);
        Info info = new Info().title("Test Service").version("1.0");

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);
        swagger = builder.build();
    }

    @Test
    public void testPathGeneration() throws Exception {
        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}"));

        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/books"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/books/{bookId}"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/relationships/books"));

        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors/{authorId}"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/relationships/exclusiveAuthors"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors/{authorId}/books"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors/{authorId}/books/{bookId}"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors/{authorId}/relationships/books"));

        Assertions.assertTrue(swagger.getPaths().containsKey("/book"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/book/{bookId}"));

        Assertions.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors/{authorId}"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/relationships/authors"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors/{authorId}/publisher"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors/{authorId}/publisher/{publisherId}"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors/{authorId}/relationships/publisher"));

        Assertions.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/publisher"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/publisher/{publisherId}"));
        Assertions.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/relationships/publisher"));

        Assertions.assertEquals(22, swagger.getPaths().size());
    }

    @Test
    public void testOperationGeneration() throws Exception {
        /* For each path, ensure the correct operations exist */
        swagger.getPaths().forEach((url, path) -> {

            /* All paths should have a GET */
            Assertions.assertNotNull(path.getGet());

            if (url.contains("relationship")) { //Relationship URL

                /* The relationship is a one to one (so there is no DELETE op */
                if ("/book/{bookId}/relationships/publisher".equals(url)) {
                    Assertions.assertNull(path.getDelete());
                    Assertions.assertNull(path.getPost());
                } else {
                    Assertions.assertNotNull(path.getDelete());
                    Assertions.assertNotNull(path.getPost());
                }
                Assertions.assertNotNull(path.getPatch());
            } else if (url.endsWith("Id}")) { //Instance URL
                Assertions.assertNotNull(path.getDelete());
                Assertions.assertNotNull(path.getPatch());
                Assertions.assertNull(path.getPost());
            } else { //Collection URL
                Assertions.assertNull(path.getDelete());
                Assertions.assertNull(path.getPatch());
                Assertions.assertNotNull(path.getPost());
            }
        });
    }

    @Test
    public void testPathParams() throws Exception {
        Path path = swagger.getPaths().get("/book/{bookId}/authors/{authorId}");
        Assertions.assertEquals(2,
                path.getParameters().stream()
                .filter((param) -> param.getIn().equals("path"))
                .count());

        Parameter bookId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("bookId"))
                .findFirst()
                .get();

        Assertions.assertEquals("path", bookId.getIn());
        Assertions.assertEquals(true, bookId.getRequired());

        Parameter authorId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("authorId"))
                .findFirst()
                .get();

        Assertions.assertEquals("path", authorId.getIn());
        Assertions.assertEquals(true, authorId.getRequired());

        path = swagger.getPaths().get("/book/{bookId}/authors");
        Assertions.assertEquals(1,
                path.getParameters().stream()
                .filter((param) -> param.getIn().equals("path"))
                .count());

        bookId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("bookId"))
                .findFirst()
                .get();

        Assertions.assertEquals("path", bookId.getIn());
        Assertions.assertEquals(true, bookId.getRequired());

        path = swagger.getPaths().get("/book/{bookId}/relationships/authors");
        Assertions.assertEquals(1,
                path.getParameters().stream()
                .filter((param) -> param.getIn().equals("path"))
                .count());

        bookId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("bookId"))
                .findFirst()
                .get();

        Assertions.assertEquals("path", bookId.getIn());
        Assertions.assertEquals(true, bookId.getRequired());
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

            Assertions.assertNotNull(bodyParam);
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

            Assertions.assertFalse(bodyParam.isPresent());
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
            Assertions.assertNotNull(bodyParam);
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
        Assertions.assertNull(response.getSchema());

        response = swagger.getPaths().get("/book/{bookId}").getDelete().getResponses().get("204");
        Assertions.assertNull(response.getSchema());

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getGet().getResponses().get("200");
        verifyDataRelationship(response.getSchema(), "author");

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getPost().getResponses().get("201");
        verifyDataRelationship(response.getSchema(), "author");

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getPatch().getResponses().get("204");
        Assertions.assertNull(response.getSchema());

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getDelete().getResponses().get("204");
        Assertions.assertNull(response.getSchema());
    }

    @Test
    public void testOperationSuccessResponseCodes() throws Exception {
        /* For each path, ensure the correct operations exist */
        swagger.getPaths().forEach((url, path) -> {

            Operation getOperation = path.getGet();
            Assertions.assertTrue(getOperation.getResponses().containsKey("200"));

            if (url.contains("relationship")) { //Relationship URL

                if (path.getDelete() != null) {
                    Operation deleteOperation = path.getDelete();
                    Assertions.assertTrue(deleteOperation.getResponses().containsKey("204"));
                }

                if (path.getPost() != null) {
                    Operation postOperation = path.getPost();
                    Assertions.assertTrue(postOperation.getResponses().containsKey("201"));
                }

                Operation patchOperation = path.getPatch();
                Assertions.assertTrue(patchOperation.getResponses().containsKey("204"));
            } else if (url.endsWith("Id}")) { //Instance URL
                Operation deleteOperation = path.getDelete();
                Assertions.assertTrue(deleteOperation.getResponses().containsKey("204"));

                Operation patchOperation = path.getPatch();
                Assertions.assertTrue(patchOperation.getResponses().containsKey("204"));
            } else { //Collection URL
                Operation postOperation = path.getPost();
                Assertions.assertTrue(postOperation.getResponses().containsKey("201"));
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
        Assertions.assertEquals(24, filterParams);

        Assertions.assertTrue(paramNames.contains("filter"));
        Assertions.assertTrue(paramNames.contains("filter[book]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][in]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][not]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][prefix]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][infix]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][postfix]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][isnull]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][notnull]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][lt]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][gt]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][le]"));
        Assertions.assertTrue(paramNames.contains("filter[book.title][ge]"));


        /* Test relationships filters */
        params = swagger.getPaths().get("/book/{bookId}/relationships/authors").getGet().getParameters();
        paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        filterParams = paramNames.stream().filter((name) -> name.startsWith("filter")).count();
        Assertions.assertEquals(23, filterParams);

        Assertions.assertTrue(paramNames.contains("filter[author]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][in]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][not]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][prefix]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][infix]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][postfix]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][isnull]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][notnull]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][lt]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][gt]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][le]"));
        Assertions.assertTrue(paramNames.contains("filter[author.name][ge]"));
    }

    @Test
    public void testPageParam() throws Exception {
        /* Tests root collection */
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long pageParams = paramNames.stream().filter((name) -> name.startsWith("page")).count();
        Assertions.assertEquals(5, pageParams);

        Assertions.assertTrue(paramNames.contains("page[number]"));
        Assertions.assertTrue(paramNames.contains("page[size]"));
        Assertions.assertTrue(paramNames.contains("page[offset]"));
        Assertions.assertTrue(paramNames.contains("page[limit]"));
        Assertions.assertTrue(paramNames.contains("page[totals]"));

        /* Tests relationship collection */
        params = swagger.getPaths().get("/book/{bookId}/relationships/authors").getGet().getParameters();

        paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        pageParams = paramNames.stream().filter((name) -> name.startsWith("page")).count();
        Assertions.assertEquals(5, pageParams);

        Assertions.assertTrue(paramNames.contains("page[number]"));
        Assertions.assertTrue(paramNames.contains("page[size]"));
        Assertions.assertTrue(paramNames.contains("page[offset]"));
        Assertions.assertTrue(paramNames.contains("page[limit]"));
        Assertions.assertTrue(paramNames.contains("page[totals]"));
    }

    @Test
    public void testSortParam() throws Exception {
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long sortParams = paramNames.stream().filter((name) -> name.startsWith("sort")).count();
        Assertions.assertEquals(1, sortParams);
        Assertions.assertTrue(paramNames.contains("sort"));

        QueryParameter sortParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("sort"))
                .findFirst()
                .get();

        Assertions.assertEquals("query", sortParam.getIn());

        List<String> sortValues = Arrays.asList("id", "-id", "title", "-title");
        Assertions.assertTrue(((StringProperty) sortParam.getItems()).getEnum().containsAll(sortValues));
        Assertions.assertEquals("csv", sortParam.getCollectionFormat());
    }

    @Test
    public void testIncludeParam() throws Exception {
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long includeParams = paramNames.stream().filter((name) -> name.startsWith("include")).count();
        Assertions.assertEquals(1, includeParams);
        Assertions.assertTrue(paramNames.contains("include"));

        QueryParameter includeParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("include"))
                .findFirst()
                .get();

        Assertions.assertEquals("query", includeParam.getIn());

        List<String> includeValues = Arrays.asList("authors", "publisher");
        Assertions.assertTrue(((StringProperty) includeParam.getItems()).getEnum().containsAll(includeValues));
        Assertions.assertEquals("csv", includeParam.getCollectionFormat());
    }

    @Test
    public void testSparseFieldsParam() throws Exception {
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long fieldParams = paramNames.stream().filter((name) -> name.startsWith("fields")).count();
        Assertions.assertEquals(1, fieldParams);
        Assertions.assertTrue(paramNames.contains("fields[book]"));

        QueryParameter fieldParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("fields[book]"))
                .findFirst()
                .get();

        Assertions.assertEquals("query", fieldParam.getIn());

        List<String> filterValues = Arrays.asList("title", "authors", "publisher");
        Assertions.assertTrue(((StringProperty) fieldParam.getItems()).getEnum().containsAll(filterValues));
        Assertions.assertEquals("csv", fieldParam.getCollectionFormat());
    }

    @Test
    public void testDefinitionGeneration() throws Exception {
        Map<String, Model> definitions = swagger.getDefinitions();

        Assertions.assertEquals(4, definitions.size());
        Assertions.assertTrue(definitions.containsKey("book"));
        Assertions.assertTrue(definitions.containsKey("author"));
        Assertions.assertTrue(definitions.containsKey("publisher"));
        Assertions.assertTrue(definitions.containsKey("Address"));

        Model bookModel = definitions.get("book");
        Assertions.assertTrue(bookModel instanceof Resource);

        ObjectProperty attributeProps = (ObjectProperty) bookModel.getProperties().get("attributes");
        Assertions.assertTrue(attributeProps.getProperties().containsKey("title"));

        ObjectProperty relationProps = (ObjectProperty) bookModel.getProperties().get("relationships");
        Assertions.assertTrue(relationProps.getProperties().containsKey("publisher"));
        Assertions.assertTrue(relationProps.getProperties().containsKey("authors"));
    }

    @Test
    public void testTagGeneration() throws Exception {

        /* Check for the global tag definitions */
        Assertions.assertEquals(3, swagger.getTags().size());

        Tag bookTag = swagger.getTags().stream()
                .filter((tag) -> tag.getName().equals("book"))
                .findFirst().get();

        Assertions.assertNotNull(bookTag);

        Tag publisherTag = swagger.getTags().stream()
                .filter((tag) -> tag.getName().equals("publisher"))
                .findFirst().get();

        Assertions.assertNotNull(publisherTag);

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
                .version("1.0");

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
                        Assertions.assertEquals(response, op.getResponses().get(key));
                    }
            );
        }
    }

    @Test
    public void testSortParameter() {
        @Entity
        @Include(rootLevel = true)
        class NothingToSort {
            @Id
            long name;
        }
        EntityDictionary entityDictionary = new EntityDictionary(Maps.newHashMap());

        entityDictionary.bindEntity(NothingToSort.class);
        Info info = new Info().title("Test Service").version("1.0");

        SwaggerBuilder builder = new SwaggerBuilder(entityDictionary, info);
        Swagger testSwagger = builder.build();

        List<Parameter> params = testSwagger.getPaths().get("/nothingToSort").getGet().getParameters();

        QueryParameter sortParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("sort"))
                .findFirst()
                .get();

        Assertions.assertEquals("query", sortParam.getIn());

        List<String> sortValues = Arrays.asList("id", "-id");
        Assertions.assertEquals(sortValues, ((StringProperty) sortParam.getItems()).getEnum());
    }


    /**
     * Verifies that the given property is of type 'Data' containing a reference to the given model.
     * @param property The property to check
     * @param refTypeName The model name
     */
    private void verifyData(Property property, String refTypeName) {
        Assertions.assertTrue((property instanceof Data));

        ArrayProperty data = (ArrayProperty) ((Data) property).getProperties().get("data");

        RefProperty ref = (RefProperty) data.getItems();
        Assertions.assertEquals("#/definitions/" + refTypeName, ref.get$ref());
    }

    /**
     * Verifies that the given model is of type 'Data' containing a reference to the given model name.
     * @param model The model to check
     * @param refTypeName The model name to check
     */
    private void verifyData(Model model, String refTypeName) {
        Assertions.assertTrue((model instanceof com.yahoo.elide.contrib.swagger.model.Data));

        ArrayProperty data = (ArrayProperty) model.getProperties().get("model");

        RefProperty ref = (RefProperty) data.getItems();
        Assertions.assertEquals("#/definitions/" + refTypeName, ref.get$ref());
    }

    /**
     * Verifies that the given property is of type 'Datum' containing a reference to the given model.
     * @param property The property to check
     * @param refTypeName The model name
     * @param included Whether or not the datum should have an 'included' section.
     */
    private void verifyDatum(Property property, String refTypeName, boolean included) {
        Assertions.assertTrue((property instanceof Datum));

        RefProperty ref = (RefProperty) ((Datum) property).getProperties().get("data");

        Assertions.assertEquals("#/definitions/" + refTypeName, ref.get$ref());

        if (included) {
            Assertions.assertNotNull(((Datum) property).getProperties().get("included"));
        }
    }

    /**
     * Verifies that the given model is of type 'Datum' containing a reference to the given model name.
     * @param model The model to check
     * @param refTypeName The model name to check
     */
    private void verifyDatum(Model model, String refTypeName) {
        Assertions.assertTrue((model instanceof com.yahoo.elide.contrib.swagger.model.Datum));

        RefProperty ref = (RefProperty) model.getProperties().get("data");

        Assertions.assertEquals("#/definitions/" + refTypeName, ref.get$ref());
    }

    /**
     * Verifies that the given property is of type 'Data' containing a 'Relationship' with the
     * correct type field.
     * @param property The property to check
     * @param refTypeName The type field to match against
     */
    private void verifyDataRelationship(Property property, String refTypeName) {
        Assertions.assertTrue((property instanceof Data));

        ArrayProperty data = (ArrayProperty) ((Data) property).getProperties().get("data");

        Relationship relation = (Relationship) data.getItems();
        StringProperty type = (StringProperty) relation.getProperties().get("type");
        Assertions.assertTrue(type.getEnum().contains(refTypeName));
    }

     /**
     * Verifies that the given model is of type 'Data' containing a 'Relationship' with the
     * correct type field.
     * @param model The model to check
     * @param refTypeName The type field to match against
     */
    private void verifyDataRelationship(Model model, String refTypeName) {
        Assertions.assertTrue((model instanceof com.yahoo.elide.contrib.swagger.model.Data));

        ArrayProperty data = (ArrayProperty) model.getProperties().get("data");

        Relationship relation = (Relationship) data.getItems();
        StringProperty type = (StringProperty) relation.getProperties().get("type");
        Assertions.assertTrue(type.getEnum().contains(refTypeName));
    }
}
