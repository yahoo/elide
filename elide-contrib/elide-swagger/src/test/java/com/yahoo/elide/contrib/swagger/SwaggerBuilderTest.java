/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import com.yahoo.elide.contrib.swagger.model.Resource;
import com.yahoo.elide.contrib.swagger.models.Author;
import com.yahoo.elide.contrib.swagger.models.Book;
import com.yahoo.elide.contrib.swagger.models.Publisher;
import com.yahoo.elide.contrib.swagger.property.Data;
import com.yahoo.elide.contrib.swagger.property.Datum;
import com.yahoo.elide.contrib.swagger.property.Relationship;
import com.yahoo.elide.core.EntityDictionary;

import com.google.common.collect.Maps;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

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


public class SwaggerBuilderTest {
    EntityDictionary dictionary;
    Swagger swagger;

    @BeforeSuite
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
        Assert.assertTrue(swagger.getPaths().containsKey("/publisher"));
        Assert.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}"));

        Assert.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/books"));
        Assert.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/books/{bookId}"));
        Assert.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/relationships/books"));

        Assert.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors"));
        Assert.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors/{authorId}"));
        Assert.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/relationships/exclusiveAuthors"));
        Assert.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors/{authorId}/books"));
        Assert.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors/{authorId}/books/{bookId}"));
        Assert.assertTrue(swagger.getPaths().containsKey("/publisher/{publisherId}/exclusiveAuthors/{authorId}/relationships/books"));

        Assert.assertTrue(swagger.getPaths().containsKey("/book"));
        Assert.assertTrue(swagger.getPaths().containsKey("/book/{bookId}"));

        Assert.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors"));
        Assert.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors/{authorId}"));
        Assert.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/relationships/authors"));
        Assert.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors/{authorId}/publisher"));
        Assert.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors/{authorId}/publisher/{publisherId}"));
        Assert.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/authors/{authorId}/relationships/publisher"));

        Assert.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/publisher"));
        Assert.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/publisher/{publisherId}"));
        Assert.assertTrue(swagger.getPaths().containsKey("/book/{bookId}/relationships/publisher"));

        Assert.assertEquals(swagger.getPaths().size(), 22);
    }

    @Test
    public void testOperationGeneration() throws Exception {
        /* For each path, ensure the correct operations exist */
        swagger.getPaths().forEach((url, path) -> {

            /* All paths should have a GET */
            Assert.assertNotNull(path.getGet());

            if (url.contains("relationship")) { //Relationship URL

                /* The relationship is a one to one (so there is no DELETE op */
                if ("/book/{bookId}/relationships/publisher".equals(url)) {
                    Assert.assertNull(path.getDelete());
                    Assert.assertNull(path.getPost());
                } else {
                    Assert.assertNotNull(path.getDelete());
                    Assert.assertNotNull(path.getPost());
                }
                Assert.assertNotNull(path.getPatch());
            } else if (url.endsWith("Id}")) { //Instance URL
                Assert.assertNotNull(path.getDelete());
                Assert.assertNotNull(path.getPatch());
                Assert.assertNull(path.getPost());
            } else { //Collection URL
                Assert.assertNull(path.getDelete());
                Assert.assertNull(path.getPatch());
                Assert.assertNotNull(path.getPost());
            }
        });
    }

    @Test
    public void testPathParams() throws Exception {
        Path path = swagger.getPaths().get("/book/{bookId}/authors/{authorId}");
        Assert.assertEquals(path.getParameters().stream()
                .filter((param) -> param.getIn().equals("path"))
                .count(), 2);

        Parameter bookId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("bookId"))
                .findFirst()
                .get();

        Assert.assertEquals(bookId.getIn(), "path");
        Assert.assertEquals(bookId.getRequired(), true);

        Parameter authorId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("authorId"))
                .findFirst()
                .get();

        Assert.assertEquals(authorId.getIn(), "path");
        Assert.assertEquals(authorId.getRequired(), true);

        path = swagger.getPaths().get("/book/{bookId}/authors");
        Assert.assertEquals(path.getParameters().stream()
                .filter((param) -> param.getIn().equals("path"))
                .count(), 1);

        bookId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("bookId"))
                .findFirst()
                .get();

        Assert.assertEquals(bookId.getIn(), "path");
        Assert.assertEquals(bookId.getRequired(), true);

        path = swagger.getPaths().get("/book/{bookId}/relationships/authors");
        Assert.assertEquals(path.getParameters().stream()
                .filter((param) -> param.getIn().equals("path"))
                .count(), 1);

        bookId = path.getParameters().stream()
                .filter((param) -> param.getName().equals("bookId"))
                .findFirst()
                .get();

        Assert.assertEquals(bookId.getIn(), "path");
        Assert.assertEquals(bookId.getRequired(), true);
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

            Assert.assertNotNull(bodyParam);
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

             Assert.assertFalse(bodyParam.isPresent());
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
            Assert.assertNotNull(bodyParam);
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
        Assert.assertNull(response.getSchema());

        response = swagger.getPaths().get("/book/{bookId}").getDelete().getResponses().get("204");
        Assert.assertNull(response.getSchema());

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getGet().getResponses().get("200");
        verifyDataRelationship(response.getSchema(), "author");

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getPost().getResponses().get("201");
        verifyDataRelationship(response.getSchema(), "author");

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getPatch().getResponses().get("204");
        Assert.assertNull(response.getSchema());

        response = swagger.getPaths().get("/book/{bookId}/relationships/authors").getDelete().getResponses().get("204");
        Assert.assertNull(response.getSchema());
    }

    @Test
    public void testOperationSuccessResponseCodes() throws Exception {
        /* For each path, ensure the correct operations exist */
        swagger.getPaths().forEach((url, path) -> {

            Operation getOperation = path.getGet();
            Assert.assertTrue(getOperation.getResponses().containsKey("200"));

            if (url.contains("relationship")) { //Relationship URL

                if (path.getDelete() != null) {
                    Operation deleteOperation = path.getDelete();
                    Assert.assertTrue(deleteOperation.getResponses().containsKey("204"));
                }

                if (path.getPost() != null) {
                    Operation postOperation = path.getPost();
                    Assert.assertTrue(postOperation.getResponses().containsKey("201"));
                }

                Operation patchOperation = path.getPatch();
                Assert.assertTrue(patchOperation.getResponses().containsKey("204"));
            } else if (url.endsWith("Id}")) { //Instance URL
                Operation deleteOperation = path.getDelete();
                Assert.assertTrue(deleteOperation.getResponses().containsKey("204"));

                Operation patchOperation = path.getPatch();
                Assert.assertTrue(patchOperation.getResponses().containsKey("204"));
            } else { //Collection URL
                Operation postOperation = path.getPost();
                Assert.assertTrue(postOperation.getResponses().containsKey("201"));
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
        Assert.assertEquals(filterParams, 13);

        Assert.assertTrue(paramNames.contains("filter"));
        Assert.assertTrue(paramNames.contains("filter[book]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][in]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][not]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][prefix]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][infix]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][postfix]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][isnull]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][notnull]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][lt]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][gt]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][le]"));
        Assert.assertTrue(paramNames.contains("filter[book.title][ge]"));


        /* Test relationships filters */
        params = swagger.getPaths().get("/book/{bookId}/relationships/authors").getGet().getParameters();
        paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        filterParams = paramNames.stream().filter((name) -> name.startsWith("filter")).count();
        Assert.assertEquals(filterParams, 12);

        Assert.assertTrue(paramNames.contains("filter[author]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][in]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][not]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][prefix]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][infix]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][postfix]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][isnull]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][notnull]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][lt]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][gt]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][le]"));
        Assert.assertTrue(paramNames.contains("filter[author.name][ge]"));
    }

    @Test
    public void testPageParam() throws Exception {
        /* Tests root collection */
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long pageParams = paramNames.stream().filter((name) -> name.startsWith("page")).count();
        Assert.assertEquals(pageParams, 5);

        Assert.assertTrue(paramNames.contains("page[number]"));
        Assert.assertTrue(paramNames.contains("page[size]"));
        Assert.assertTrue(paramNames.contains("page[offset]"));
        Assert.assertTrue(paramNames.contains("page[limit]"));
        Assert.assertTrue(paramNames.contains("page[totals]"));

        /* Tests relationship collection */
        params = swagger.getPaths().get("/book/{bookId}/relationships/authors").getGet().getParameters();

        paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        pageParams = paramNames.stream().filter((name) -> name.startsWith("page")).count();
        Assert.assertEquals(pageParams, 5);

        Assert.assertTrue(paramNames.contains("page[number]"));
        Assert.assertTrue(paramNames.contains("page[size]"));
        Assert.assertTrue(paramNames.contains("page[offset]"));
        Assert.assertTrue(paramNames.contains("page[limit]"));
        Assert.assertTrue(paramNames.contains("page[totals]"));
    }

    @Test
    public void testSortParam() throws Exception {
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long sortParams = paramNames.stream().filter((name) -> name.startsWith("sort")).count();
        Assert.assertEquals(sortParams, 1);
        Assert.assertTrue(paramNames.contains("sort"));

        QueryParameter sortParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("sort"))
                .findFirst()
                .get();

        Assert.assertEquals(sortParam.getIn(), "query");

        List<String> sortValues = Arrays.asList("title", "-title");
        Assert.assertTrue(((StringProperty) sortParam.getItems()).getEnum().containsAll(sortValues));
        Assert.assertEquals(sortParam.getCollectionFormat(), "csv");
    }

    @Test
    public void testIncludeParam() throws Exception {
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long includeParams = paramNames.stream().filter((name) -> name.startsWith("include")).count();
        Assert.assertEquals(includeParams, 1);
        Assert.assertTrue(paramNames.contains("include"));

        QueryParameter includeParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("include"))
                .findFirst()
                .get();

        Assert.assertEquals(includeParam.getIn(), "query");

        List<String> sortValues = Arrays.asList("authors", "publisher");
        Assert.assertTrue(((StringProperty) includeParam.getItems()).getEnum().containsAll(sortValues));
        Assert.assertEquals(includeParam.getCollectionFormat(), "csv");
    }

    @Test
    public void testSparseFieldsParam() throws Exception {
        List<Parameter> params = swagger.getPaths().get("/book").getGet().getParameters();

        Set<String> paramNames = params.stream()
                .map((param) -> param.getName())
                .collect(Collectors.toSet());

        long fieldParams = paramNames.stream().filter((name) -> name.startsWith("fields")).count();
        Assert.assertEquals(fieldParams, 1);
        Assert.assertTrue(paramNames.contains("fields[book]"));

        QueryParameter fieldParam = (QueryParameter) params.stream()
                .filter((param) -> param.getName().equals("fields[book]"))
                .findFirst()
                .get();

        Assert.assertEquals(fieldParam.getIn(), "query");

        List<String> sortValues = Arrays.asList("title", "authors", "publisher");
        Assert.assertTrue(((StringProperty) fieldParam.getItems()).getEnum().containsAll(sortValues));
        Assert.assertEquals(fieldParam.getCollectionFormat(), "csv");
    }

    @Test
    public void testDefinitionGeneration() throws Exception {
        Map<String, Model> definitions = swagger.getDefinitions();

        Assert.assertEquals(definitions.size(), 4);
        Assert.assertTrue(definitions.containsKey("book"));
        Assert.assertTrue(definitions.containsKey("author"));
        Assert.assertTrue(definitions.containsKey("publisher"));
        Assert.assertTrue(definitions.containsKey("Address"));

        Model bookModel = definitions.get("book");
        Assert.assertTrue(bookModel instanceof Resource);

        ObjectProperty attributeProps = (ObjectProperty) bookModel.getProperties().get("attributes");
        Assert.assertTrue(attributeProps.getProperties().containsKey("title"));

        ObjectProperty relationProps = (ObjectProperty) bookModel.getProperties().get("relationships");
        Assert.assertTrue(relationProps.getProperties().containsKey("publisher"));
        Assert.assertTrue(relationProps.getProperties().containsKey("authors"));
    }

    @Test
    public void testTagGeneration() throws Exception {

        /* Check for the global tag definitions */
        Assert.assertEquals(swagger.getTags().size(), 3);

        Tag bookTag = swagger.getTags().stream()
                .filter((tag) -> tag.getName().equals("book"))
                .findFirst().get();

        Assert.assertNotNull(bookTag);

        Tag publisherTag = swagger.getTags().stream()
                .filter((tag) -> tag.getName().equals("publisher"))
                .findFirst().get();

        Assert.assertNotNull(publisherTag);

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
                        Assert.assertEquals(op.getResponses().get(key), response);
                    }
            );
        }
    }

    /**
     * Verifies that the given property is of type 'Data' containing a reference to the given model.
     * @param property The property to check
     * @param refTypeName The model name
     */
    private void verifyData(Property property, String refTypeName) {
        Assert.assertTrue((property instanceof Data));

        ArrayProperty data = (ArrayProperty) ((Data) property).getProperties().get("data");

        RefProperty ref = (RefProperty) data.getItems();
        Assert.assertEquals(ref.get$ref(), "#/definitions/" + refTypeName);
    }

    /**
     * Verifies that the given model is of type 'Data' containing a reference to the given model name.
     * @param model The model to check
     * @param refTypeName The model name to check
     */
    private void verifyData(Model model, String refTypeName) {
        Assert.assertTrue((model instanceof com.yahoo.elide.contrib.swagger.model.Data));

        ArrayProperty data = (ArrayProperty) model.getProperties().get("model");

        RefProperty ref = (RefProperty) data.getItems();
        Assert.assertEquals(ref.get$ref(), "#/definitions/" + refTypeName);
    }

    /**
     * Verifies that the given property is of type 'Datum' containing a reference to the given model.
     * @param property The property to check
     * @param refTypeName The model name
     * @param included Whether or not the datum should have an 'included' section.
     */
    private void verifyDatum(Property property, String refTypeName, boolean included) {
        Assert.assertTrue((property instanceof Datum));

        RefProperty ref = (RefProperty) ((Datum) property).getProperties().get("data");

        Assert.assertEquals(ref.get$ref(), "#/definitions/" + refTypeName);

        if (included) {
            Assert.assertNotNull(((Datum) property).getProperties().get("included"));
        }
    }

    /**
     * Verifies that the given model is of type 'Datum' containing a reference to the given model name.
     * @param model The model to check
     * @param refTypeName The model name to check
     */
    private void verifyDatum(Model model, String refTypeName) {
        Assert.assertTrue((model instanceof com.yahoo.elide.contrib.swagger.model.Datum));

        RefProperty ref = (RefProperty) model.getProperties().get("data");

        Assert.assertEquals(ref.get$ref(), "#/definitions/" + refTypeName);
    }

    /**
     * Verifies that the given property is of type 'Data' containing a 'Relationship' with the
     * correct type field.
     * @param property The property to check
     * @param refTypeName The type field to match against
     */
    private void verifyDataRelationship(Property property, String refTypeName) {
        Assert.assertTrue((property instanceof Data));

        ArrayProperty data = (ArrayProperty) ((Data) property).getProperties().get("data");

        Relationship relation = (Relationship) data.getItems();
        StringProperty type = (StringProperty) relation.getProperties().get("type");
        Assert.assertTrue(type.getEnum().contains(refTypeName));
    }

     /**
     * Verifies that the given model is of type 'Data' containing a 'Relationship' with the
     * correct type field.
     * @param model The model to check
     * @param refTypeName The type field to match against
     */
    private void verifyDataRelationship(Model model, String refTypeName) {
        Assert.assertTrue((model instanceof com.yahoo.elide.contrib.swagger.model.Data));

        ArrayProperty data = (ArrayProperty) model.getProperties().get("data");

        Relationship relation = (Relationship) data.getItems();
        StringProperty type = (StringProperty) relation.getProperties().get("type");
        Assert.assertTrue(type.getEnum().contains(refTypeName));
    }
}
