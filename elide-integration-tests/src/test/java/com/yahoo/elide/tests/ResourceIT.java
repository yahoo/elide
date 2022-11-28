/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION;
import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.document;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.include;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static com.yahoo.elide.test.jsonapi.elements.Relation.TO_ONE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.audit.TestAuditLogger;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.filter.predicates.InfixPredicate;
import com.yahoo.elide.core.filter.predicates.PostfixPredicate;
import com.yahoo.elide.core.filter.predicates.PrefixPredicate;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.utils.JsonParser;
import com.yahoo.elide.initialization.IntegrationTest;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.test.jsonapi.elements.Data;
import com.yahoo.elide.test.jsonapi.elements.Resource;
import com.google.common.collect.Sets;
import example.Address;
import example.Book;
import example.Child;
import example.Company;
import example.ExceptionThrowingBean;
import example.FunWithPermissions;
import example.Invoice;
import example.LineItem;
import example.Parent;
import example.TestCheckMappings;
import example.User;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.owasp.encoder.Encode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response.Status;

/**
 * The type Config resource test.
 */
public class ResourceIT extends IntegrationTest {
    private static final Resource COMPANY = resource(
            type("company"),
            id("abc"),
            attributes(
                    attr("address", buildAddress("street1", null, Map.of("foo", "bar"))),
                    attr("description", "ABC")
            )
    );
    private static final Resource PARENT1 = resource(
            type("parent"),
            id("1"),
            attributes(
                    attr("firstName", null)
            ),
            relationships(
                    relation("children",
                            linkage(type("child"), id("1"))
                    ),
                    relation("spouses")
            )
    );
    private static final Resource PARENT2 = resource(
            type("parent"),
            id("2"),
            attributes(
                    attr("firstName", "John")
            ),
            relationships(
                    relation("children",
                            linkage(type("child"), id("2")),
                            linkage(type("child"), id("3"))
                    ),
                    relation("spouses")
            )
    );
    private static final Resource PARENT3 = resource(
            type("parent"),
            id("3"),
            attributes(
                    attr("firstName", "Link")
            ),
            relationships(
                    relation("children",
                            linkage(type("child"), id("4")),
                            linkage(type("child"), id("5"))
                    ),
                    relation("spouses")
            )
    );
    private static final Resource PARENT4 = resource(
            type("parent"),
            id("4"),
            attributes(
                    attr("firstName", "Unknown")
            ),
            relationships(
                    relation("children"),
                    relation("spouses",
                            linkage(type("parent"), id("3"))
                    )
            )
    );
    private static final Resource CHILD1 = resource(
            type("child"),
            id("1"),
            attributes(
                    attr("name", null)
            ),
            relationships(
                    relation("friends"),
                    relation("parents",
                            linkage(type("parent"), id("1"))
                    )
            )
    );
    private static final Resource CHILD2 = resource(
            type("child"),
            id("2"),
            attributes(
                    attr("name", "Child-ID2")
            ),
            relationships(
                    relation("friends",
                            linkage(type("child"), id("3"))
                    ),
                    relation("parents",
                            linkage(type("parent"), id("2"))
                    )
            )
    );
    private static final Resource CHILD3 = resource(
            type("child"),
            id("3"),
            attributes(
                    attr("name", "Child-ID3")
            ),
            relationships(
                    relation("friends"),
                    relation("parents",
                            linkage(type("parent"), id("2"))
                    )
            )
    );
    private static final Resource CHILD4 = resource(
            type("child"),
            id("4"),
            attributes(
                    attr("name", null)
            ),
            relationships(
                    relation("friends"),
                    relation("parents",
                            linkage(type("parent"), id("3"))
                    )
            )
    );
    private static final Resource CHILD5 = resource(
            type("child"),
            id("5"),
            attributes(
                    attr("name", null)
            ),
            relationships(
                    relation("friends"),
                    relation("parents",
                            linkage(type("parent"), id("3"))
                    )
            )
    );
    private final JsonParser jsonParser = new JsonParser();
    private final String baseUrl = "http://localhost:8080/api";

    private static Address buildAddress(String street1, String street2, Map<Object, Object> properties) {
        final Address address = new Address();
        address.setStreet1(street1);
        address.setStreet2(street2);
        address.setProperties(properties);
        return address;
    }

    private static Stream<Arguments> likeQueryProvider() {
        return Stream.of(
                Arguments.of("filter[book.title][infix]=with%perce", 1),
                Arguments.of("filter[book.title][prefix]=titlewith%perce", 1),
                Arguments.of("filter[book.title][postfix]=with%percentage", 1)
        );
    }

    private static Stream<Arguments> queryProviderHQL() {
        Path.PathElement pathToTitle = new Path.PathElement(Book.class, String.class, "title");

        return Stream.of(
                Arguments.of(new InfixPredicate(pathToTitle, "with%perce"), 1),
                Arguments.of(new PrefixPredicate(pathToTitle, "titlewith%perce"), 1),
                Arguments.of(new PostfixPredicate(pathToTitle, "with%percentage"), 1)
        );
    }

    @BeforeEach
    public void setup() throws IOException {
        dataStore.populateEntityDictionary(EntityDictionary.builder().build());
        DataStoreTransaction tx = dataStore.beginTransaction();

        final Company company = new Company();
        company.setId("abc");
        company.setDescription("ABC");

        Address address = new Address();
        address.setStreet1("foo");
        address.setProperties(Map.of("boo", "baz"));

        company.setAddress(address);

        tx.createObject(company, null);

        Parent parent = new Parent(); // id 1
        Child child = new Child(); // id 1
        parent.setChildren(Sets.newHashSet(child));
        parent.setSpouses(Sets.newHashSet());
        child.setParents(Sets.newHashSet(parent));

        tx.createObject(parent, null);
        tx.createObject(child, null);

        // Single tests
        Parent p1 = new Parent(); // id 2
        p1.setFirstName("John");
        p1.setSpouses(Sets.newHashSet());

        Child c1 = new Child(); // id 2
        c1.setName("Child-ID2");
        c1.setParents(Sets.newHashSet(p1));

        Child c2 = new Child(); // id 3
        c2.setName("Child-ID3");
        c2.setParents(Sets.newHashSet(p1));

        Set<Child> friendSet = new HashSet<>();
        friendSet.add(c2);
        c1.setFriends(friendSet);

        Set<Child> childrenSet1 = new HashSet<>();
        childrenSet1.add(c1);
        childrenSet1.add(c2);

        p1.setChildren(childrenSet1);

        // List tests
        Parent p2 = new Parent(); // id 3
        Parent p3 = new Parent();  // id 4
        Child c3 = new Child(); // id 4
        c3.setParents(Sets.newHashSet(p2));
        Child c4 = new Child(); // id 5
        c4.setParents(Sets.newHashSet(p2));
        Set<Child> childrenSet2 = new HashSet<>();
        childrenSet2.add(c3);
        childrenSet2.add(c4);

        p2.setChildren(childrenSet2);
        p2.setFirstName("Link");

        p3.setFirstName("Unknown");

        p2.setSpouses(Sets.newHashSet());
        p3.setSpouses(Sets.newHashSet(p2));
        p3.setChildren(Sets.newHashSet());

        tx.createObject(c1, null);
        tx.createObject(c2, null);
        tx.createObject(c3, null);
        tx.createObject(c4, null);

        tx.createObject(p1, null);
        tx.createObject(p2, null);
        tx.createObject(p3, null);

        Book bookWithPercentage = new Book();
        bookWithPercentage.setTitle("titlewith%percentage");
        Book bookWithoutPercentage = new Book();
        bookWithoutPercentage.setTitle("titlewithoutpercentage");

        tx.createObject(bookWithPercentage, null);
        tx.createObject(bookWithoutPercentage, null);

        FunWithPermissions fun = new FunWithPermissions();
        tx.createObject(fun, null);

        User user = new User(); //ID 1
        user.setPassword("god");
        tx.createObject(user, null);

        Invoice invoice = new Invoice();
        invoice.setId(1);
        LineItem item = new LineItem();
        invoice.setItems(Sets.newHashSet(item));
        item.setInvoice(invoice);
        tx.createObject(invoice, null);
        tx.createObject(item, null);

        ExceptionThrowingBean etb = new ExceptionThrowingBean();
        etb.setId(1L);
        tx.createObject(etb, null);

        tx.commit(null);
        tx.close();
    }

    @Test
    public void testRootCollection() throws Exception {
        String actual = given().when().get("/parent").then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        JsonApiDocument doc = jsonApiMapper.readJsonApiDocument(actual);
        assertEquals(4, doc.getData().get().size());
    }

    @Test
    public void testRootCollectionWithNoOperatorFilter() throws Exception {
        String actual = given().when().get("/parent?filter[parent.id][isnull]").then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();

        JsonApiDocument doc = jsonApiMapper.readJsonApiDocument(actual);
        assertEquals(0, doc.getData().get().size());
    }

    @Test
    public void testReadPermissionWithFilterExpressionCheckCollectionId() {
        // To see the detail of the FilterExpression check, go to the bean of filterExpressionCheckObj and see
        // CheckRestrictUser.
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("filterExpressionCheckObj"),
                                        id(null),
                                        attributes(attr("name", "obj1"))
                                )
                        )
                )
                .post("/filterExpressionCheckObj")
                .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("1"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("filterExpressionCheckObj"),
                                        id("2"),
                                        attributes(attr("name", "obj2"))
                                )
                        )
                )
                .post("/filterExpressionCheckObj")
                .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("2"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("filterExpressionCheckObj"),
                                        id("3"),
                                        attributes(attr("name", "obj3"))
                                )
                        )
                )
                .post("/filterExpressionCheckObj")
                .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("3"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("anotherFilterExpressionCheckObj"),
                                        id("1"),
                                        attributes(
                                                attr("anotherName", "anotherObj1"),
                                                attr("createDate", "1999")
                                        ),
                                        relationships(
                                                relation("linkToParent",
                                                        linkage(type("filterExpressionCheckObj"), id("1"))
                                                )
                                        )
                                )
                        )
                )
                .post("/anotherFilterExpressionCheckObj")
                .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("1"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("anotherFilterExpressionCheckObj"),
                                        attributes(
                                                attr("anotherName", "anotherObj2"),
                                                attr("createDate", "2000")
                                        )
                                )
                        )
                )
                .post("/anotherFilterExpressionCheckObj")
                .then().statusCode(HttpStatus.SC_CREATED).body("data.id", equalTo("2"));

        //The User ID is set to one so the following get request won't return record including
        // filterExpressionCheckObj.id != User'id.
        //test root object collection, should just receive 2 out of 3 records.
        String expected1 = "{\"data\":[{\"type\":\"filterExpressionCheckObj\",\"id\":\"1\","
                + "\"attributes\":{\"name\":\"obj1\"},"
                + "\"relationships\":{\"listOfAnotherObjs\":{"
                + "\"data\":[{\"type\":\"anotherFilterExpressionCheckObj\",\"id\":\"1\"}]}}},{"
                + "\"type\":\"filterExpressionCheckObj\",\"id\":\"2\",\"relationships\":{"
                + "\"listOfAnotherObjs\":{\"data\":[]}}}]}";

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/filterExpressionCheckObj")
                .then().statusCode(HttpStatus.SC_OK).body(equalTo(expected1));

        //test authentication pass querying with ID == 1
        String expected2 = "{\"data\":{\"type\":\"filterExpressionCheckObj\",\"id\":\"1\","
                + "\"attributes\":{\"name\":\"obj1\"},\"relationships\":{\"listOfAnotherObjs\":{\"data\":[{\"type"
                + "\":\"anotherFilterExpressionCheckObj\",\"id\":\"1\"}]}}}}";
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/filterExpressionCheckObj/1")
                .then().statusCode(HttpStatus.SC_OK).body(equalTo(expected2));

        //test authentication pass querying with ID == 2, it shouldn't contain attribute "name".
        String expected3 = "{\"data\":{\"type\":\"filterExpressionCheckObj\",\"id\":\"2\","
                + "\"relationships\":{\"listOfAnotherObjs\":{\"data\":[]}}}}";
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/filterExpressionCheckObj/2")
                .then().statusCode(HttpStatus.SC_OK).body(equalTo(expected3));

        //test authentication fail querying with ID == 3
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/filterExpressionCheckObj/3")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("errors[0].detail", equalTo("Unknown identifier 3 for filterExpressionCheckObj"));

        //test authentication pass query a relation of object
        String expected5 = "{\"data\":[{\"type\":\"anotherFilterExpressionCheckObj\",\"id\":\"1\","
                + "\"attributes\":{\"anotherName\":\"anotherObj1\",\"createDate\":1999},\"relationships\":{"
                + "\"linkToParent\":{\"data\":[{\"type\":\"filterExpressionCheckObj\",\"id\":\"1\"}]}}}]}";
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/filterExpressionCheckObj/1/listOfAnotherObjs")
                .then().statusCode(HttpStatus.SC_OK).body(equalTo(expected5));

        //test authentication pass query a relation of object
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/anotherFilterExpressionCheckObj")
                .then().statusCode(HttpStatus.SC_OK).body(equalTo(expected5));
    }

    @Test
    public void testRootCollectionId() {
        given()
                .when()
                .get("/parent/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        datum(PARENT1).toJSON()));
    }

    @Test
    public void testRootCollectionRelationships() {
        given()
                .when().get("/parent/1/relationships/children").then().statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        data(linkage(type("child"), id("1"))).toJSON()));
    }

    @Test
    public void testChild() throws Exception {
        given().when().get("/parent/1/children/1").then().statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(CHILD1).toJSON()));
    }

    @Test
    public void testSubCollectionRelationships() throws Exception {
        given().when().get("/parent/1/children/1/relationships/parents").then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        data(linkage(type("parent"), id("1"))).toJSON()));
    }

    @Test
    public void failRootCollectionRelationships() {
        given().when().get("/parent/1/relationships").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void failRootCollection() throws Exception {
        String expected = "{\"errors\":[{\"detail\":\"Unknown collection unknown\"}]}";

        given().when().get("/unknown").then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(equalTo(expected));
    }

    @Test
    public void failRootCollectionId() {
        String expected = "{\"errors\":[{\"detail\":\"Unknown identifier 6789 for parent\"}]}";

        given().when().get("/parent/6789").then().statusCode(HttpStatus.SC_NOT_FOUND)
                .body(equalTo(expected));
    }

    @Test
    public void failChild() throws Exception {
        String expected = "{\"errors\":[{\"detail\":\"Unknown collection unknown\"}]}";

        given().when().get("/parent/1/unknown").then().statusCode(HttpStatus.SC_NOT_FOUND)
                .body(equalTo(expected));
    }

    @Test
    public void failFieldRequest() throws Exception {
        String expected = "{\"errors\":[{\"detail\":\"Unknown collection id\"}]}";

        given().when().get("/parent/1/id").then().statusCode(HttpStatus.SC_NOT_FOUND)
                .body(equalTo(expected));
    }

    @Test
    public void parseFailure() {
        String expected = "{\"errors\":[{\"detail\":\"token recognition error at: &#39;|&#39;\"}]}";

        given().when().get("company/1|apps/2/links/foo").then().statusCode(HttpStatus.SC_NOT_FOUND)
                .body(equalTo(expected));
    }

    @Test
    public void testPatchAttrSingle() throws Exception {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("parent"),
                                        id("2"),
                                        attributes(
                                                attr("firstName", "syzygy")
                                        )
                                )
                        )
                )
                .patch("/parent/2")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(equalTo(
                        datum(
                                resource(
                                        type("parent"),
                                        id("2"),
                                        attributes(
                                                attr("firstName", "syzygy")
                                        ),
                                        relationships(
                                                relation("children",
                                                        linkage(type("child"), id("2")),
                                                        linkage(type("child"), id("3"))
                                                ),
                                                relation("spouses")
                                        )
                                )
                        ).toJSON()
                ));
    }

    @Test
    public void testPatchComplexAttribute() {

        final Address update = new Address();
        update.setStreet1("street1");
        update.setProperties(Map.of("foo", "bar"));

        // update company address
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("company"),
                                        id("abc"),
                                        attributes(
                                                attr("address", update)
                                        )
                                )
                        )
                )
                .patch("/company/abc")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/company/abc")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(equalTo(datum(COMPANY).toJSON()
                ));
    }

    @Test
    public void testPatchAttrList() throws Exception {
        String request = "{\n"
                + "    \"data\":[\n"
                + "        {\n"
                + "            \"type\":\"parent\",\n"
                + "            \"id\":\"3\",\n"
                + "            \"attributes\":{\n"
                + "                \"firstName\":\"Senor\"\n"
                + "            }\n"
                + "        },\n"
                + "        {\n"
                + "            \"type\":\"parent\",\n"
                + "            \"id\":\"11\",\n"
                + "            \"attributes\":{\n"
                + "                \"firstName\":\"woot\"\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "}";

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .patch("/parent/3")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testPatchSetRel() throws Exception {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("parent"),
                                        id("4"),
                                        attributes(),
                                        relationships(
                                                relation("children",
                                                        linkage(type("child"), id("4")),
                                                        linkage(type("child"), id("5"))
                                                )
                                        )
                                )
                        )

                )
                .patch("/parent/4")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/4")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        datum(
                                resource(
                                        type("parent"),
                                        id("4"),
                                        attributes(
                                                attr("firstName", "Unknown")
                                        ),
                                        relationships(
                                                relation("children",
                                                        linkage(type("child"), id("4")),
                                                        linkage(type("child"), id("5"))
                                                ),
                                                relation("spouses",
                                                        linkage(type("parent"), id("3"))
                                                )
                                        )
                                )
                        ).toJSON()
                ));
    }

    @Test
    public void testPatchRemoveRelSingle() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("parent"),
                                        id("2"),
                                        attributes(),
                                        relationships(
                                                relation("children",
                                                        linkage(type("child"), id("2"))
                                                )
                                        )
                                )
                        )

                )
                .patch("/parent/2")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        datum(
                                resource(
                                        type("parent"),
                                        id("2"),
                                        attributes(
                                                attr("firstName", "John")
                                        ),
                                        relationships(
                                                relation("children",
                                                        linkage(type("child"), id("2"))
                                                ),
                                                relation("spouses")
                                        )
                                )
                        ).toJSON()
                ));
    }

    @Test
    public void testPatchRelNoUpdateSingle() {
        //Test a relationship update that leaves the resource unchanged.
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(PARENT2))
                .patch("/parent/2")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(PARENT2).toJSON()
                ));
    }

    @Test
    public void testPatchRelRemoveColl() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("parent"),
                                        id("2"),
                                        attributes(),
                                        relationships(
                                                relation("children")
                                        )
                                )
                        )
                )
                .patch("/parent/2")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        datum(
                                resource(
                                        type("parent"),
                                        id("2"),
                                        attributes(
                                                attr("firstName", "John")
                                        ),
                                        relationships(
                                                relation("children"),
                                                relation("spouses")
                                        )
                                )
                        ).toJSON()
                ));
    }

    @Test
    public void testGetNestedSingleInclude() throws Exception {
        String expected = document(
                datum(PARENT2),
                include(CHILD2, CHILD3)).toJSON();

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2?include=children.friends")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(expected, false));
    }

    @Test
    public void testGetSingleIncludeOnCollection() throws Exception {

        String expected = document(
                data(PARENT1, PARENT2, PARENT3, PARENT4),
                include(CHILD1, CHILD2, CHILD3, CHILD4, CHILD5)).toJSON();

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?include=children")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(expected, false));
    }

    @Test
    public void testGetMultipleIncludeOnCollection() throws Exception {
        String expected = document(
                data(PARENT1, PARENT2, PARENT3, PARENT4),
                include(CHILD1, CHILD2, CHILD3, CHILD4, CHILD5, PARENT3)).toJSON();

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?include=children,spouses")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(expected, false));
    }

    @Test
    public void testGetSingleIncludeOnRelationship() {
        String expected = document(
                data(linkage(type("child"), id("1"))),
                include(CHILD1)).toJSON();

        given()
                .when().get("/parent/1/relationships/children?include=children").then().statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void testGetIncludeBadRelation() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/1?include=children.BadRelation")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testMissingTypeInJsonBody() {
        String detail = "Resource 'type' field is missing or empty.";

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{ \"data\": { \"id\": \"1\", \"attributes\": { \"firstName\": \"foo\" }}}")
                .patch("/parent/1")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors[0].detail", equalTo(Encode.forHtml(detail)));
    }

    @Test
    public void testInvalidJson() {
        String detail = "Unexpected close marker ']': expected '}' (for Object starting at [Source: (String)\"{ ]\"; line: 1, column: 1])\n at [Source: (String)\"{ ]\"; line: 1, column: 4]";

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{ ]")
                .patch("/parent/1")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors[0].detail", equalTo(Encode.forHtml(detail)));
    }

    @Test
    public void testGetSortCollection() throws Exception {

        String expected = data(PARENT1, PARENT2, PARENT3, PARENT4).toJSON();

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?sort=+firstName")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void testGetReverseSortCollection() throws Exception {

        String expected = data(PARENT4, PARENT3, PARENT2, PARENT1).toJSON();

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?sort=-firstName")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void testGetRelEmptyColl() {
        String expected = data(null).toJSON();

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/4/relationships/children")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void testGetWithTrailingSlash() {
        String expected = data(PARENT1, PARENT2, PARENT3, PARENT4).toJSON();

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void testPatchRelSetDirect() throws Exception {

        Data relationships = data(
                linkage(type("child"), id("4")),
                linkage(type("child"), id("5"))
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(relationships)
                .patch("/parent/4/relationships/children")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/4/relationships/children")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(relationships, false));
    }

    @Test
    public void testNoDeleteExcludedRelationship() throws Exception {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{\"data\":{\"type\":\"excludedRelationship\",\"id\":\"1\"}}")
                .delete("/parent/4/children/4/relationships/excludedRelationship")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testForbiddenDeleteEmptyCollectionRelationship() throws Exception {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body("{\"data\":[]}")
                .delete("/parent/1/children/1/relationships/parents")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testDeleteParent() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .delete("/parent/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .body(isEmptyOrNullString());
    }

    @Test
    public void testDeleteWithCascade() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .delete("/invoice/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .body(isEmptyOrNullString());
    }

    @Test
    public void failDeleteParent() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .delete("/parent/678")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void createParentNoRels() throws Exception {

        Data parent = datum(
                resource(
                        type("parent"),
                        id("5"),
                        attributes(
                                attr("firstName", "I'm new here")
                        ),
                        relationships(
                                relation("spouses"),
                                relation("children")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(parent)
                .post("/parent")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(jsonEquals(parent, true));
    }

    @Test
    public void createParentWithRels() throws Exception {

        Data parentInput = datum(
                resource(
                        type("parent"),
                        id("required"),
                        attributes(
                                attr("firstName", "omg. I have kidz.")
                        ),
                        relationships(
                                relation("children",
                                        linkage(type("child"), id("2"))
                                )
                        )
                )
        );

        Data parentOutput = datum(
                resource(
                        type("parent"),
                        id("5"),
                        attributes(
                                attr("firstName", "omg. I have kidz.")
                        ),
                        relationships(
                                relation("children",
                                        linkage(type("child"), id("2"))
                                ),
                                relation("spouses")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(parentInput)
                .post("/parent")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(jsonEquals(parentOutput, true));
    }

    @Test
    public void createParentList() {
        String request = jsonParser.getJson("/ResourceIT/createParentList.req.json");
        String expected = "{\"errors\":[{\"detail\":\"Bad Request Body";

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(request)
                .post("/parent")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(startsWith(expected));
    }

    @Test
    public void createChild() throws Exception {

        Data childInput = data(
                resource(
                        type("child"),
                        id("required"),
                        attributes(),
                        relationships(
                                relation("parents",
                                        linkage(type("parent"), id("1"))
                                )
                        )
                )
        );

        Data childOutput = datum(
                resource(
                        type("child"),
                        id("6"),
                        attributes(
                                attr("name", null)
                        ),
                        relationships(
                                relation("parents",
                                        linkage(type("parent"), id("1")),
                                        linkage(type("parent"), id("4"))
                                ),
                                relation("friends")
                        )
                )
        );

        Data parentOutput = datum(
                resource(
                        type("parent"),
                        id("4"),
                        attributes(
                                attr("firstName", "Unknown")
                        ),
                        relationships(
                                relation("children",
                                        linkage(type("child"), id("6"))
                                ),
                                relation("spouses",
                                        linkage(type("parent"), id("3"))
                                )
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(childInput)
                .post("/parent/4/children")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(jsonEquals(childOutput, true));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/4")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(parentOutput, true));
    }

    @Test
    public void createParentBadUri() {

        Data parentInput = data(
                resource(
                        type("parent"),
                        id("required"),
                        attributes(
                                attr("firstName", "I should not be created :x")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(parentInput)
                .post("/parent/678")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void createChildNonRootable() {
        Data childInput = data(
                resource(
                        type("child"),
                        id("required"),
                        attributes(
                                attr("firstName", "I should not be created :x")
                        )
                )
        );
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(childInput)
                .post("/child")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testAddAndRemoveOneToOneRelationship() throws Exception {

        Data funInput = datum(
                resource(
                        type("fun"),
                        id("1"),
                        attributes(),
                        relationships(
                                relation("relation3", TO_ONE,
                                        linkage(type("child"), id("2"))
                                )
                        )
                )
        );

        Data funOutput = datum(
                resource(
                        type("fun"),
                        id("1"),
                        attributes(
                                attr("field2", null),
                                attr("field3", null),
                                attr("field4", null),
                                attr("field5", null),
                                attr("field6", null),
                                attr("field8", null)
                        ),
                        relationships(
                                relation("relation3", TO_ONE,
                                        linkage(type("child"), id("2"))
                                ),
                                relation("relation1"),
                                relation("relation2")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(funInput)
                .patch("/fun/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());

        given().when().get("/fun/1").then().statusCode(HttpStatus.SC_OK).body(jsonEquals(funOutput, true));

        funInput = datum(
                resource(
                        type("fun"),
                        id("1"),
                        attributes(),
                        relationships(
                                relation("relation3", TO_ONE)
                        )
                )
        );

        funOutput = datum(
                resource(
                        type("fun"),
                        id("1"),
                        attributes(
                                attr("field2", null),
                                attr("field3", null),
                                attr("field4", null),
                                attr("field5", null),
                                attr("field6", null),
                                attr("field8", null)
                        ),
                        relationships(
                                relation("relation3", TO_ONE),
                                relation("relation1"),
                                relation("relation2")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(funInput)
                .patch("/fun/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());

        given()
                .when()
                .get("/fun/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(funOutput, true));
    }

    @Test
    public void createDependentPatchExt() {
        String request = jsonParser.getJson("/ResourceIT/createDependentPatchExt.req.json");
        String expected = jsonParser.getJson("/ResourceIT/createDependentPatchExt.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void createChildRelateExisting() {
        String request = jsonParser.getJson("/ResourceIT/createChildRelateExisting.req.json");
        String expected = jsonParser.getJson("/ResourceIT/createChildRelateExisting.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void invalidPatchNullOp() {
        String request = jsonParser.getJson("/ResourceIT/patchExtNullOp.req.json");

        String detail = "Patch extension operation cannot be null.";

        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors[0].detail[0]", containsString(Encode.forHtml(detail)));
    }

    @Test
    public void invalidPatchMissingId() {
        String request = jsonParser.getJson("/ResourceIT/invalidPatchMissingId.req.json");

        String detail = "Bad Request Body'Patch extension requires all objects to have an assigned "
                + "ID (temporary or permanent) when assigning relationships.'";

        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors[0].detail[0]", equalTo(Encode.forHtml(detail)));
    }

    @Test
    public void invalidPatchMissingPath() {
        String request = jsonParser.getJson("/ResourceIT/invalidPatchMissingPath.req.json");

        String detail = "Bad Request Body'Patch extension requires all objects to have an assigned path'";

        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("errors[0].detail[0]", equalTo(Encode.forHtml(detail)));
    }

    @Test
    public void updateChildRelationToExisting() {
        String request = jsonParser.getJson("/ResourceIT/updateChildRelationToExisting.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/updateChildRelationToExisting.1.json");
        String expected2 = jsonParser.getJson("/ResourceIT/updateChildRelationToExisting.2.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected1));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/4/children/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected2));
    }

    @Test
    public void replaceAttributesAndRelationship() {
        String request = jsonParser.getJson("/ResourceIT/replaceAttributesAndRelationship.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/replaceAttributesAndRelationship.json");
        String expected2 = jsonParser.getJson("/ResourceIT/replaceAttributesAndRelationship.2.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected1));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(expected2, false));
    }

    @Test
    public void removeObject() {
        String req1 = jsonParser.getJson("/ResourceIT/removeObject.1.req.json");
        String req2 = jsonParser.getJson("/ResourceIT/removeObject.2.req.json");
        String expectedDirect = jsonParser.getJson("/ResourceIT/removeObject.direct.json");
        String expected1 = jsonParser.getJson("/ResourceIT/removeObject.1.json");
        String expected2 = jsonParser.getJson("/ResourceIT/removeObject.2.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(req1)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected1));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/5")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expectedDirect));
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(req2)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected2));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/5")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void createAndRemoveParent() {
        String request = jsonParser.getJson("/ResourceIT/createAndRemoveParent.req.json");
        String expected = jsonParser.getJson("/ResourceIT/createAndRemoveParent.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/4")
                .then()
                .statusCode(HttpStatus.SC_OK);
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/4")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testAddRoot() {
        String request = jsonParser.getJson("/ResourceIT/testAddRoot.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/testAddRoot.1.json");
        String expected2 = jsonParser.getJson("/ResourceIT/testAddRoot.2.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected1));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/5")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected2));
    }

    @Test
    public void updateRelationshipDirect() {
        String request = jsonParser.getJson("/ResourceIT/updateRelationshipDirect.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/updateRelationshipDirect.1.json");
        String expected2 = jsonParser.getJson("/ResourceIT/updateRelationshipDirect.2.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent/1/relationships/children")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected1));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(expected2, true));
    }

    @Test
    public void removeSingleRelationship() {
        String request = jsonParser.getJson("/ResourceIT/removeSingleRelationship.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/removeSingleRelationship.1.json");
        String expected2 = jsonParser.getJson("/ResourceIT/removeSingleRelationship.2.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent/2/relationships/children")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected1));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(expected2, false));
    }

    @Test
    public void addRelationshipChild() throws Exception {
        Data expected = datum(
                resource(
                        type("child"),
                        id("1"),
                        attributes(
                                attr("name", null)
                        ),
                        relationships(
                                relation("friends"),
                                relation("parents",
                                        linkage(type("parent"), id("1")),
                                        linkage(type("parent"), id("2"))
                                )
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                linkage(type("parent"), id("2"))
                        )
                )
                .post("/parent/1/children/1/relationships/parents")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/1/children/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(expected, false));
    }

    @Test
    public void removeRelationshipChild() {
        Data expected = datum(
                resource(
                        type("child"),
                        id("1"),
                        attributes(
                                attr("name", null)
                        ),
                        relationships(
                                relation("friends"),
                                relation("parents")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(
                        linkage(type("parent"), id("1"))
                ))
                .delete("/parent/1/children/1/relationships/parents")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/child/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected.toJSON()));
    }

    @Test
    public void addRelationships() {
        String request = jsonParser.getJson("/ResourceIT/addRelationships.req.json");
        String expected1 = jsonParser.getJson("/ResourceIT/addRelationships.json");
        String expected2 = jsonParser.getJson("/ResourceIT/addRelationships.2.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent/1/relationships/children")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected1));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(expected2, false));
    }

    @Test
    public void checkJsonApiPatchWithError() {
        String request = jsonParser.getJson("/ResourceIT/checkJsonApiPatchWithError.req.json");
        String expected = jsonParser.getJson("/ResourceIT/checkJsonApiPatchWithError.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void patchExtBadId() {
        String request = jsonParser.getJson("/ResourceIT/patchExtBadId.req.json");
        String expected = jsonParser.getJson("/ResourceIT/patchExtBadId.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expected));
    }

    @Test
    public void patchExtAddUpdate() {
        String request = jsonParser.getJson("/ResourceIT/patchExtAddUpdate.req.json");
        String expected = jsonParser.getJson("/ResourceIT/patchExtAddUpdate.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    @Tag("skipInMemory")
    //Verifies violation of unique column constraint.
    public void patchExtBadValue() throws IOException {
        String request = jsonParser.getJson("/ResourceIT/patchExtBadValue.req.json");

        jsonApiMapper.getObjectMapper().readTree(given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/")
                .then()
                .statusCode(anyOf(equalTo(HttpStatus.SC_LOCKED), equalTo(HttpStatus.SC_BAD_REQUEST)))
                .extract()
                .body()
                .asString());
    }

    @Test
    public void patchExtBadDelete() {
        String request = jsonParser.getJson("/ResourceIT/patchExtBadDelete.req.json");
        String expected = jsonParser.getJson("/ResourceIT/patchExtBadDelete.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(jsonEquals(expected, false));
    }

    @Test
    public void createParentWithoutId() {
        Data newParent = data(
                resource(
                        type("parent"),
                        attributes(
                                attr("firstName", "omg. I have kidz.")
                        ),
                        relationships(
                                relation("children",
                                        linkage(type("child"), id("2"))
                                ),
                                relation("spouses")
                        )
                )
        );

        Data expected = new Data(
                resource(
                        type("parent"),
                        id("5"),
                        attributes(
                                attr("firstName", "omg. I have kidz.")
                        ),
                        relationships(
                                relation("children",
                                        linkage(type("child"), id("2"))
                                ),
                                relation("spouses")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(newParent)
                .post("/parent")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(jsonEquals(expected, false));
    }

    @Test
    public void testOneToOneRelationshipAdding() {

        Data oneToOneRoot = datum(
                resource(
                        type("oneToOneRoot"),
                        id("1"),
                        attributes(
                                attr("name", "test123")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(oneToOneRoot)
                .post("/oneToOneRoot")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Verify it was actually created
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(oneToOneRoot)
                .get("/oneToOneRoot/1")
                .then()
                .statusCode(HttpStatus.SC_OK);

        Data oneToOneNonRoot = datum(
                resource(
                        type("oneToOneNonRoot"),
                        id("1"),
                        attributes(
                                attr("test", "Other object")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(oneToOneNonRoot)
                .post("/oneToOneRoot/1/otherObject")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Verify contents
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/oneToOneRoot/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        datum(
                                resource(
                                        type("oneToOneRoot"),
                                        id("1"),
                                        attributes(
                                                attr("name", "test123")
                                        ),
                                        relationships(
                                                relation("otherObject", TO_ONE,
                                                        linkage(type("oneToOneNonRoot"), id("1"))
                                                )
                                        )
                                )
                        ).toJSON()
                ));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/oneToOneRoot/1/otherObject")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        datum(
                                resource(
                                        type("oneToOneNonRoot"),
                                        id("1"),
                                        attributes(
                                                attr("test", "Other object")
                                        ),
                                        relationships(
                                                relation("root", TO_ONE,
                                                        linkage(type("oneToOneRoot"), id("1"))
                                                )
                                        )
                                )
                        ).toJSON()
                ));
    }

    @Test
    public void testReadPermissionDefaultOverride() throws Exception {
        Resource obj = resource(
                type("yetAnotherPermission"),
                id("1"),
                attributes(
                        attr("youShouldBeAbleToRead", "this!")
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(obj))
                .post("/yetAnotherPermission")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Verify contents
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/yetAnotherPermission")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(jsonEquals(data(obj), true));
    }

    @Test
    public void testUpdateToOneCollection() throws Exception {
        Data oneToOneRoot = datum(
                resource(
                        type("oneToOneRoot"),
                        id("1"),
                        attributes(
                                attr("name", "test123")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(oneToOneRoot)
                .post("/oneToOneRoot")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        Data oneToOneNonRoot = datum(
                resource(
                        type("oneToOneNonRoot"),
                        id("1"),
                        attributes(
                                attr("test", "Other object")
                        )
                )
        );

        // Create other object
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(oneToOneNonRoot)
                .post("/oneToOneRoot/1/otherObject")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Verify contents
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/oneToOneRoot/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        datum(
                                resource(
                                        type("oneToOneRoot"),
                                        id("1"),
                                        attributes(
                                                attr("name", "test123")

                                        ),
                                        relationships(
                                                relation("otherObject", TO_ONE,
                                                        linkage(type("oneToOneNonRoot"), id("1"))
                                                )
                                        )
                                )
                        ).toJSON()
                ));

        Data updated = datum(
                resource(
                        type("oneToOneNonRoot"),
                        id("2"),
                        attributes(
                                attr("test", "Another object")
                        )
                )
        );

        // Create another object
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(updated)
                .post("/oneToOneRoot/1/otherObject")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Verify contents
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/oneToOneRoot/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        datum(
                                resource(
                                        type("oneToOneRoot"),
                                        id("1"),
                                        attributes(
                                                attr("name", "test123")

                                        ),
                                        relationships(
                                                relation("otherObject", TO_ONE,
                                                        linkage(type("oneToOneNonRoot"), id("2"))
                                                )
                                        )
                                )
                        ).toJSON()
                ));
    }

    @Test
    public void testPostToRecord() {
        Data oneToOneRoot = datum(
                resource(
                        type("oneToOneRoot"),
                        id("1"),
                        attributes(
                                attr("name", "test123")
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(oneToOneRoot)
                .post("/oneToOneRoot")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(oneToOneRoot)
                .post("/oneToOneRoot/1")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testFilterIds() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2/relationships/children?filter[child.id]=3")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        data(
                                linkage(type("child"), id("3"))
                        ).toJSON()));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2?include=children&filter[child.id]=3")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        document(
                                datum(
                                        resource(
                                                type("parent"),
                                                id("2"),
                                                attributes(
                                                        attr("firstName", "John")
                                                ),
                                                relationships(
                                                        relation("children",
                                                                linkage(type("child"), id("3"))
                                                        ),
                                                        relation("spouses")
                                                )
                                        )
                                ),
                                include(CHILD3)
                        ).toJSON()
                ));
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?include=children&filter[child.id]=4")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        document(
                                data(
                                        resource(
                                                type("parent"),
                                                id("1"),
                                                attributes(
                                                        attr("firstName", null)
                                                ),
                                                relationships(
                                                        relation("children"),
                                                        relation("spouses")
                                                )

                                        ),
                                        resource(
                                                type("parent"),
                                                id("2"),
                                                attributes(
                                                        attr("firstName", "John")
                                                ),
                                                relationships(
                                                        relation("children"),
                                                        relation("spouses")
                                                )

                                        ),
                                        resource(
                                                type("parent"),
                                                id("3"),
                                                attributes(
                                                        attr("firstName", "Link")
                                                ),
                                                relationships(
                                                        relation("children",
                                                                linkage(type("child"), id("4"))
                                                        ),
                                                        relation("spouses")
                                                )

                                        ),
                                        resource(
                                                type("parent"),
                                                id("4"),
                                                attributes(
                                                        attr("firstName", "Unknown")
                                                ),
                                                relationships(
                                                        relation("children"),
                                                        relation("spouses",
                                                                linkage(type("parent"), id("3"))
                                                        )
                                                )

                                        )
                                ),
                                include(CHILD4)
                        ).toJSON()));
    }

    @Test
    public void testIssue608() {
        String req = jsonParser.getJson("/ResourceIT/Issue608.req.json");
        String expected = jsonParser.getJson("/ResourceIT/Issue608.resp.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(req)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void testNestedPatch() {
        String req = jsonParser.getJson("/ResourceIT/nestedPatchCreate.req.json");
        String expected = jsonParser.getJson("/ResourceIT/nestedPatchCreate.resp.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(req)
                .patch("/parent")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void testCreatedRootNoReadPermRequired() {
        String req = jsonParser.getJson("/ResourceIT/testPatchExtNoReadPermForNew.req.json");
        String badReq = "[{\n"
                + "    \"op\": \"add\","
                + "    \"path\": \"/1/child\",\n"
                + "    \"value\": {\n"
                + "      \"type\": \"child\",\n"
                + "      \"id\": \"12345678-1234-1234-1234-123456789ab2\"\n"
                + "    }\n"
                + "  }]";
        String expected = jsonParser.getJson("/ResourceIT/testPatchExtNoReadPermForNew.resp.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(req)
                .patch("/specialread")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(badReq)
                .patch("/specialread")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo("[{\"errors\":[{\"detail\":\"UpdatePermission Denied\",\"status\":\"403\"}]}]"));
    }

    @Test
    public void testUserNoShare() {
        Resource noShareBid1 = resource(
                type("noShareBid"),
                id("1")
        );

        Resource noShareBid2 = resource(
                type("noShareBid"),
                id("2"),
                relationships(
                        relation("other", TO_ONE,
                                linkage(type("noShareBid"), id("1"))
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(noShareBid1))
                .post("/noShareBid")
                .then()
                .statusCode(HttpStatus.SC_CREATED);
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(noShareBid2))
                .post("/noShareBid/1/other")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(datum(noShareBid2).toJSON()));
    }

    @Test
    public void testPatchExtNoCommit() {
        String req = jsonParser.getJson("/ResourceIT/testPatchExtNoCommit.req.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(req)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(equalTo("{\"errors\":[{\"detail\":\"CreatePermission Denied\"}]}"));
    }

    @Test
    public void testInverseDeleteFromCollection() {
        Data parentOutput = datum(
                resource(
                        type("parent"),
                        id("5"),
                        attributes(
                                attr("firstName", "omg. I have kidz.")
                        ),
                        relationships(
                                relation("children",
                                        linkage(type("child"), id("4"))
                                ),
                                relation("spouses")
                        )
                )
        );

        //Create parent 5 and assign it to child 4 (for the strange security check).
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(parentOutput)
                .post("/parent")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // NOTE: This only tests this behavior is correct BECAUSE of the Child4Parent5 check.
        // It's a bit contrived, but we shouldn't lose the logic.
        // The problem: when deleting an inverse relation, it checks whether it can update its inverse field back
        // to the original. This is flawed logic since you're deleting the original in the first place (and that check
        // succeeded if we got there).
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .delete("/parent/5/children/4")
                .then()
                .statusCode(204);
    }

    @Test
    public void testPostInvalidRelationship() {
        // Note: This tests the correct response when POSTing a resource with a not "include" relationship. The server
        // should returns UnknownEntityException rather than NPE.
        Resource invalid = resource(
                type("resourceWithInvalidRelationship"),
                id("1"),
                attributes(
                        attr("name", "test123")
                ),
                relationships(
                        relation("notIncludedResource",
                                linkage(type("notIncludedResource"), id("1"))
                        )
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(invalid))
                .post("resourceWithInvalidRelationship")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testSortByIdRootableTopLevel() {
        given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?sort=id")
                .then()
                .body(equalTo(data(PARENT1, PARENT2, PARENT3, PARENT4).toJSON()));

        given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent?sort=-id")
                .then()
                .body(equalTo(data(PARENT4, PARENT3, PARENT2, PARENT1).toJSON()));
    }

    @Test
    public void testSortByIdNonRootableTopLevel() {
        given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2/children?sort=id")
                .then()
                .body(equalTo(data(CHILD2, CHILD3).toJSON()));

        given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/parent/2/children?sort=-id")
                .then()
                .body(equalTo(data(CHILD3, CHILD2).toJSON()));
    }

    @Test
    public void testExceptionThrowingBean() {
        // Ensure web exception from bean gets bubbled up
        given()
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/exceptionThrowingBean/1")
                .then()
                .statusCode(Status.GONE.getStatusCode());
    }

    @Test
    public void assignedIdString() {

        Resource resource = resource(
                type("assignedIdString"),
                id("user1"),
                attributes(
                        attr("value", 22)
                )
        );

        //Create user with assigned id
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(resource))
                .post("/assignedIdString")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(datum(resource).toJSON()));

        //Fetch newly created user
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/assignedIdString/user1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(resource).toJSON()));

        Resource modified = resource(
                type("assignedIdString"),
                id("user2"),
                attributes(
                        attr("value", 22)
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(modified))
                .patch("/assignedIdString/user1")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void assignedIdLong() {

        Resource resource = resource(
                type("assignedIdLong"),
                id("1"),
                attributes(
                        attr("value", 22)
                )
        );


        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(resource))
                .post("/assignedIdLong")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(datum(resource).toJSON()));

        //Fetch newly created user
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/assignedIdLong/1")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(resource).toJSON()));

        Resource modified = resource(
                type("assignedIdLong"),
                id("2"),
                attributes(
                        attr("value", 22)
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(modified))
                .patch("/assignedIdLong/1")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void assignedIdWithoutProvidedId() {
        Resource resource = resource(
                type("assignedIdString"),
                attributes(
                        attr("value", 22)
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(resource))
                .post("/assignedIdString")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void elideSecurityEnabled() {
        Elide elide = new Elide(new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(EntityDictionary.builder().checks(TestCheckMappings.MAPPINGS).build())
                .withAuditLogger(new TestAuditLogger())
                .build());

        elide.doScans();

        com.yahoo.elide.core.security.User user = new com.yahoo.elide.core.security.User(() -> "-1");
        ElideResponse response = elide.get(baseUrl, "parent/1/children", new MultivaluedHashMap<>(), user, NO_VERSION);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());
        assertEquals("{\"data\":[]}", response.getBody());
    }

    @Test
    public void testComputedAttribute() throws Exception {
        Resource patched = resource(
                type("user"),
                id("1"),
                attributes(
                        attr("password", "god")
                )
        );

        Resource returned = resource(
                type("user"),
                id("1"),
                attributes(
                        attr("password", ""),
                        attr("reversedPassword", "dog"),
                        attr("role", 0)
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(patched))
                .patch("/user/1")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .header(HttpHeaders.CONTENT_LENGTH, nullValue());

        given().when().get("/user/1").then().statusCode(HttpStatus.SC_OK)
                .body(equalTo(datum(returned).toJSON()));
    }

    @Test
    public void testPrivilegeEscalation() throws Exception {
        Resource patched = resource(
                type("user"),
                id("1"),
                attributes(
                        attr("role", 1)
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(patched))
                .patch("/user/1")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    // Update checks should be _deferred_ (neither ignored nor aggressively applied) on newly created objects.
    @Test
    public void testUpdateDeferredOnCreate() {

        Resource expected = resource(
                type("createButNoUpdate"),
                id("1"),
                attributes(
                        attr("cannotModify", "unmodified"),
                        attr("textValue", "new value")
                ),
                relationships(
                        relation("toOneRelation", true)
                )
        );

        Resource badRequest = resource(
                type("createButNoUpdate"),
                id("1"),
                attributes(
                        attr("cannotModify", "This should fail this whole create"),
                        attr("textValue", "test")
                )
        );

        Resource validRequest = resource(
                type("createButNoUpdate"),
                id("1"),
                attributes(
                        attr("textValue", "new value")
                )
        );

        // First ensure we cannot update fields that are explicitly disallowed
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(badRequest))
                .post("/createButNoUpdate")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // Now check that updating allowed fields enables us to create the object
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(validRequest))
                .post("/createButNoUpdate")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(datum(expected).toJSON()));

        // Ensure we cannot update that newly created object
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(validRequest))
                .patch("/createButNoUpdate/1")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testUpdatingExistingResourceWithoutPermissionsIsForbidden() {

        Resource entity1 = resource(
                type("createButNoUpdate"),
                id("1"),
                attributes(
                        attr("textValue", "new value")
                )
        );

        Resource entity2 = resource(
                type("createButNoUpdate"),
                id("2"),
                attributes(
                        attr("textValue", "new value")
                ),
                relationships(
                        relation("toOneRelation", linkage(type("createButNoUpdate"), id("1")))
                )
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(entity1))
                .post("/createButNoUpdate")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(datum(entity2))
                .post("/createButNoUpdate")
                .then()
                .statusCode(HttpStatus.SC_CREATED);


        Data relationships = data(
                linkage(type("createButNoUpdate"), id("1"))
        );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(relationships)
                .post("/createButNoUpdate/2/relationships/toOneRelation")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testPatchDeferredOnCreate() {
        String request = jsonParser.getJson("/ResourceIT/testPatchDeferredOnCreate.req.json");
        String expected = jsonParser.getJson("/ResourceIT/testPatchDeferredOnCreate.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .body(request)
                .patch("/")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(equalTo(expected));
    }

    @ParameterizedTest
    @MethodSource("likeQueryProvider")
    public void testSpecialCharacterLikeQuery(String filterParam, int noOfRecords) throws Exception {
        String actual = given().when().get(String.format("/book?%s", filterParam)).then().statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
        JsonApiDocument doc = jsonApiMapper.readJsonApiDocument(actual);
        assertEquals(doc.getData().get().size(), noOfRecords);

    }

    @ParameterizedTest
    @MethodSource("queryProviderHQL")
    public void testSpecialCharacterLikeQueryHQL(FilterPredicate filterPredicate, int noOfRecords) throws Exception {
        DataStoreTransaction tx = dataStore.beginReadTransaction();
        RequestScope scope = mock(RequestScope.class);
        EntityDictionary dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Book.class);
        when(scope.getDictionary()).thenReturn(dictionary);
        PaginationImpl pagination = mock(PaginationImpl.class);
        when(pagination.returnPageTotals()).thenReturn(true);
        tx.loadObjects(EntityProjection.builder()
                .type(Book.class)

                .filterExpression(filterPredicate)
                .pagination(pagination)
                .build(), scope);
        tx.commit(scope);
        tx.close();
        verify(pagination).setPageTotals((long) noOfRecords);
    }

    @Test
    public void testPaginationLimitOverrides() {
        // Well below the limit
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .get("/parent?page[size]=10")
                .then()
                .statusCode(HttpStatus.SC_OK);

        // At the limit
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .get("/parent?page[size]=100000")
                .then()
                .statusCode(HttpStatus.SC_OK);

        // Above the limit
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .get("/parent?page[size]=100001")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    // TODO: Test that user checks still apply at commit time
    @Test
    public void badRoot() {
        given().when().get("/oops").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void badRootId() {
        given().when().get("/oops/1").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void badChildCollection() {
        given().when().get("/user/1/oops").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void badChildCollectionId() {
        given().when().get("/user/1/oops/1").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @Tag("skipInMemory") //Skipping because storage for in-memory store is
    //broken out by class instead of a common table.
    public void testVersionedBookFetch() throws Exception {
        given()
                .header("ApiVersion", "1.0")
                .when()
                .get("/book?fields[book]=name")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        data(
                                resource(
                                        type("book"),
                                        id("1"),
                                        attributes(
                                                attr("name", "titlewith%percentage")
                                        )
                                ),
                                resource(
                                        type("book"),
                                        id("2"),
                                        attributes(
                                                attr("name", "titlewithoutpercentage")
                                        )
                                )
                        ).toJSON()
                ));

        given()
                .header("ApiVersion", NO_VERSION)
                .when()
                .get("/book?fields[book]=title")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        data(
                                resource(
                                        type("book"),
                                        id("1"),
                                        attributes(
                                                attr("title", "titlewith%percentage")
                                        )
                                ),
                                resource(
                                        type("book"),
                                        id("2"),
                                        attributes(
                                                attr("title", "titlewithoutpercentage")
                                        )
                                )
                        ).toJSON()
                ));
    }

    @Test
    @Tag("skipInMemory") //Skipping because storage for in-memory store is
    //broken out by class instead of a common table.
    public void testVersionedBookCreate() throws Exception {
        given()
                .header("ApiVersion", "1.0")
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("book"),
                                        attributes(
                                                attr("name", "A new book."),
                                                attr("publishDate", 1123)
                                        )
                                )
                        )
                )
                .when()
                .post("/book")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(
                        datum(
                                resource(
                                        type("book"),
                                        id("3"),
                                        attributes(
                                                attr("name", "A new book."),
                                                attr("publishDate", 1123)
                                        )
                                )
                        ).toJSON()
                ));
    }

    @Test
    @Tag("skipInMemory") //Skipping because storage for in-memory store is
    //broken out by class instead of a common table.
    public void testVersionedBookPatch() throws Exception {
        given()
                .header("ApiVersion", "1.0")
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("book"),
                                        id("2"),
                                        attributes(
                                                attr("name", "A new book.")
                                        )
                                )
                        )
                )
                .when()
                .patch("/book/2")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    @Tag("skipInMemory") //Skipping because storage for in-memory store is
    //broken out by class instead of a common table.
    public void testVersionedPatchExtension() {
        String req = jsonParser.getJson("/ResourceIT/versionedPatchExtension.req.json");
        String expected = jsonParser.getJson("/ResourceIT/versionedPatchExtension.resp.json");
        given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .header("ApiVersion", "1.0")
                .body(req)
                .patch("/book")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    @Tag("skipInMemory") //Skipping because storage for in-memory store is
    //broken out by class instead of a common table.
    public void testVersionedBookDelete() throws Exception {
        given()
                .header("ApiVersion", "1.0")
                .when()
                .delete("/book/2")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }


    @Test
    public void testMissingVersionedModels() throws Exception {
        given()
                .header("ApiVersion", "1.0")
                .when()
                .get("/parent")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
