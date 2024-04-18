/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.test.jsonapi;

import static com.paiondata.elide.test.jsonapi.JsonApiDSL.atomicOperation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.lid;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.links;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relationships;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static com.paiondata.elide.test.jsonapi.elements.PatchOperationType.add;
import static com.paiondata.elide.test.jsonapi.elements.Relation.TO_ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.test.jsonapi.elements.AtomicOperationCode;

import org.junit.jupiter.api.Test;

public class JsonApiDSLTest {

    @Test
    public void verifyBasicRequest() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":1}}";

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id(1)
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithAttributes() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\",\"attributes\":"
                + "{\"title\":\"Why You Should use Elide\",\"date\":\"2019-01-01\"}}}";

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "Why You Should use Elide"),
                                JsonApiDSL.attr("date", "2019-01-01")
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithOneToOneRelationship() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{\"author\":{\"data\":{\"type\":\"author\",\"id\":\"1\"}}}}}";

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "title")
                        ),
                        JsonApiDSL.relationships(
                                relation("author",
                                        TO_ONE,
                                        JsonApiDSL.linkage(JsonApiDSL.type("author"), JsonApiDSL.id("1"))
                                )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithOneToManyRelationship() {
        // multi-elements array of resource identifier objects for non-empty to-many relationships.
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{"
                + "\"comments\":{\"data\":[{\"type\":\"comment\",\"id\":\"1\"},"
                + "{\"type\":\"comment\",\"id\":\"2\"}]}}}}";

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "title")
                        ),
                        JsonApiDSL.relationships(
                              JsonApiDSL.relation("comments",
                                      JsonApiDSL.linkage(JsonApiDSL.type("comment"), JsonApiDSL.id("1")),
                                      JsonApiDSL.linkage(JsonApiDSL.type("comment"), JsonApiDSL.id("2"))
                              )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);

        // single-element array of resource identifier objects for non-empty to-many relationships.
        expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{"
                + "\"comments\":{\"data\":[{\"type\":\"comment\",\"id\":\"1\"}]}}}}";

        actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "title")
                        ),
                        JsonApiDSL.relationships(
                                JsonApiDSL.relation("comments",
                                        JsonApiDSL.linkage(JsonApiDSL.type("comment"), JsonApiDSL.id("1"))
                                )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithManyRelationships() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},"
                + "\"relationships\":{"
                + "\"author\":{\"data\":[{\"type\":\"author\",\"id\":\"1\"}]},"
                + "\"comments\":{\"data\":[{\"type\":\"comment\",\"id\":\"2\"}]}}}}";

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "title")
                        ),
                        JsonApiDSL.relationships(
                                JsonApiDSL.relation("author",
                                        JsonApiDSL.linkage(JsonApiDSL.type("author"), JsonApiDSL.id("1"))
                                ),
                                JsonApiDSL.relation("comments",
                                        JsonApiDSL.linkage(JsonApiDSL.type("comment"), JsonApiDSL.id("2"))
                                )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyResourcesWithIncludes() {
        String expected = "{\"data\":[{\"type\":\"blog\",\"id\":1}],\"included\":[{\"type\":\"author\",\"id\":1}]}";

        String actual = JsonApiDSL.document(
                JsonApiDSL.data(
                    JsonApiDSL.resource(
                            JsonApiDSL.type("blog"),
                            JsonApiDSL.id(1)
                    )
                ),
                JsonApiDSL.include(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("author"),
                                JsonApiDSL.id(1)
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyDataWithRelationshipLinks() {
        String expected = "{\"data\":[{\"type\":\"blog\",\"id\":1},{\"type\":\"blog\",\"id\":2}]}";

        String actual =
                JsonApiDSL.data(
                        JsonApiDSL.linkage(
                                JsonApiDSL.type("blog"),
                                JsonApiDSL.id(1)
                        ),
                        JsonApiDSL.linkage(
                                JsonApiDSL.type("blog"),
                                JsonApiDSL.id(2)
                        )
                ).toJSON();


        assertEquals(expected, actual);
    }

    @Test
    public void verifyEmptyToOneRelationship() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},\"relationships\":{\"author\":{\"data\":null}}}}";

        String actual =
                JsonApiDSL.datum(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("blog"),
                                JsonApiDSL.id("1"),
                                JsonApiDSL.attributes(
                                        JsonApiDSL.attr("title", "title")
                                ),
                                JsonApiDSL.relationships(
                                        relation("author", TO_ONE)
                                )
                        )
                ).toJSON();


        assertEquals(expected, actual);
    }

    @Test
    public void verifyEmptyToManyRelationship() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\","
                + "\"attributes\":{\"title\":\"title\"},\"relationships\":{\"comments\":{\"data\":[]}}}}";

        String actual =
                JsonApiDSL.datum(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("blog"),
                                JsonApiDSL.id("1"),
                                JsonApiDSL.attributes(
                                        JsonApiDSL.attr("title", "title")
                                ),
                                JsonApiDSL.relationships(
                                        JsonApiDSL.relation("comments")
                                )
                        )
                ).toJSON();


        assertEquals(expected, actual);
    }

    @Test
    public void verifyRelationshipsButNoAttributes() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\",\"relationships\":{\"comments\":{\"data\":[]},"
                + "\"author\":{\"data\":[{\"type\":\"author\",\"id\":\"1\"}]}}}}";

        String actual =
                JsonApiDSL.datum(
                        JsonApiDSL.resource(
                                JsonApiDSL.type("blog"),
                                JsonApiDSL.id("1"),
                                JsonApiDSL.relationships(
                                        JsonApiDSL.relation("comments"),
                                        JsonApiDSL.relation("author",
                                                JsonApiDSL.linkage(JsonApiDSL.type("author"), JsonApiDSL.id("1"))
                                        )
                                )
                        )
                ).toJSON();


        assertEquals(expected, actual);
    }

    @Test
    public void verifyNoId() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":null,\"attributes\""
                + ":{\"title\":\"Why You Should use Elide\",\"date\":\"2019-01-01\"}}}";

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "Why You Should use Elide"),
                                JsonApiDSL.attr("date", "2019-01-01")
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyPatchOperation() {
        String expected = "[{\"op\":\"add\",\"path\":\"/parent\",\"value\":{\"type\":\"parent\",\"id\":\"1\",\"relationships\":{\"children\":{\"data\":[{\"type\":\"child\",\"id\":\"2\"}]},\"spouses\":{\"data\":[{\"type\":\"parent\",\"id\":\"3\"}]}}}},{\"op\":\"add\",\"path\":\"/parent/1/children\",\"value\":{\"type\":\"child\",\"id\":\"2\"}}]";

        String actual = JsonApiDSL.patchSet(
                JsonApiDSL.patchOperation(add, "/parent",
                        JsonApiDSL.resource(
                                JsonApiDSL.type("parent"),
                                JsonApiDSL.id("1"),
                                JsonApiDSL.relationships(
                                        JsonApiDSL.relation("children",
                                                JsonApiDSL.linkage(JsonApiDSL.type("child"), JsonApiDSL.id("2"))
                                        ),
                                        JsonApiDSL.relation("spouses",
                                                JsonApiDSL.linkage(JsonApiDSL.type("parent"), JsonApiDSL.id("3"))
                                        )
                                )
                        )
                ),
                JsonApiDSL.patchOperation(add, "/parent/1/children",
                        JsonApiDSL.resource(
                                JsonApiDSL.type("child"),
                                JsonApiDSL.id("2")
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithLinks() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\",\"attributes\":"
                + "{\"title\":\"Why You Should use Elide\",\"date\":\"2019-01-01\"},"
                + "\"links\":{\"self\":\"http://localhost:8080/json/api/v1/blog/1\"}}}";

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "Why You Should use Elide"),
                                JsonApiDSL.attr("date", "2019-01-01")
                        ),
                        JsonApiDSL.links(
                                JsonApiDSL.attr("self", "http://localhost:8080/json/api/v1/blog/1")
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }

    @Test
    public void verifyRequestWithLinksRelationship() {
        String expected = "{\"data\":{\"type\":\"blog\",\"id\":\"1\",\"attributes\":"
                + "{\"title\":\"Why You Should use Elide\"},"
                + "\"relationships\":{"
                +   "\"author\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/api/v1/blog/1/relationships/author\",\"related\":\"http://localhost:8080/json/api/v1/blog/1/author\"},"
                +       "\"data\":[{\"type\":\"author\",\"id\":\"1\"}]"
                +   "},"
                +   "\"comments\":{"
                +       "\"links\":{\"self\":\"http://localhost:8080/json/api/v1/blog/1/relationships/comments\",\"related\":\"http://localhost:8080/json/api/v1/blog/1/comments\"},"
                +       "\"data\":[]"
                +   "}},"
                + "\"links\":{\"self\":\"http://localhost:8080/json/api/v1/blog/1\"}}}";

        String actual = JsonApiDSL.datum(
                JsonApiDSL.resource(
                        JsonApiDSL.type("blog"),
                        JsonApiDSL.id("1"),
                        JsonApiDSL.attributes(
                                JsonApiDSL.attr("title", "Why You Should use Elide")
                        ),
                        JsonApiDSL.links(
                                JsonApiDSL.attr("self", "http://localhost:8080/json/api/v1/blog/1")
                        ),
                        JsonApiDSL.relationships(
                                JsonApiDSL.relation("author",
                                        JsonApiDSL.links(
                                                JsonApiDSL.attr("self", "http://localhost:8080/json/api/v1/blog/1/relationships/author"),
                                                JsonApiDSL.attr("related", "http://localhost:8080/json/api/v1/blog/1/author")
                                        ),
                                        JsonApiDSL.linkage(JsonApiDSL.type("author"), JsonApiDSL.id("1"))
                                ),
                                JsonApiDSL.relation("comments",
                                        JsonApiDSL.links(
                                                JsonApiDSL.attr("self", "http://localhost:8080/json/api/v1/blog/1/relationships/comments"),
                                                JsonApiDSL.attr("related", "http://localhost:8080/json/api/v1/blog/1/comments")
                                        )
                                )
                        )
                )
        ).toJSON();

        assertEquals(expected, actual);
    }
    @Test
    void verifyAtomicOperation() {
        String expected = """
                {"atomic:operations":[{"op":"add","href":"/parent","data":{"type":"parent","id":"1","relationships":{"children":{"data":[{"type":"child","id":"2"}]},"spouses":{"data":[{"type":"parent","id":"3"}]}}}},{"op":"add","href":"/parent/1/children","data":{"type":"child","id":"2"}}]}""";
        String actual = JsonApiDSL.atomicOperations(
                    JsonApiDSL.atomicOperation(AtomicOperationCode.add, "/parent",
                            JsonApiDSL.datum(
                            JsonApiDSL.resource(
                                    JsonApiDSL.type("parent"),
                                    JsonApiDSL.id("1"),
                                    JsonApiDSL.relationships(
                                            JsonApiDSL.relation("children",
                                                    JsonApiDSL.linkage(JsonApiDSL.type("child"), JsonApiDSL.id("2"))
                                            ),
                                            JsonApiDSL.relation("spouses",
                                                    JsonApiDSL.linkage(JsonApiDSL.type("parent"), JsonApiDSL.id("3"))
                                            )
                                    )
                            ))
                    ),
                    JsonApiDSL.atomicOperation(AtomicOperationCode.add, "/parent/1/children",
                            JsonApiDSL.datum(
                            JsonApiDSL.resource(
                                    JsonApiDSL.type("child"),
                                    JsonApiDSL.id("2")
                            ))
                    )
                ).toJSON();
        assertEquals(expected, actual);
    }

    @Test
    void verifyAtomicOperationData() {
        String expected = """
                {"atomic:operations":[{"op":"add","data":{"type":"article","id":"13","attributes":{"name":"article"}}}]}""";
        String actual = JsonApiDSL.atomicOperations(
                atomicOperation(AtomicOperationCode.add,
                        datum(resource(
                                type("article"),
                                id("13"),
                                attributes(
                                        attr("name", "article")
                                )
                        ))
                )
                ).toJSON();
        assertEquals(expected, actual);
    }

    @Test
    void verifyAtomicOperationDataLid() {
        String expected = """
                {"atomic:operations":[{"op":"add","data":{"type":"article","lid":"13"}}]}""";
        String actual = JsonApiDSL.atomicOperations(
                atomicOperation(AtomicOperationCode.add,
                        datum(resource(
                                type("article"),
                                lid("13")
                        ))
                )
                ).toJSON();
        assertEquals(expected, actual);
    }

    @Test
    void verifyAtomicOperationDataAttributesLid() {
        String expected = """
                {"atomic:operations":[{"op":"add","data":{"type":"article","lid":"13","attributes":{"name":"article"}}}]}""";
        String actual = JsonApiDSL.atomicOperations(
                atomicOperation(AtomicOperationCode.add,
                        datum(resource(
                                type("article"),
                                lid("13"),
                                attributes(
                                        attr("name", "article")
                                )
                        ))
                )
                ).toJSON();
        assertEquals(expected, actual);
    }

    @Test
    void verifyAtomicOperationDataAttributesRelationshipsLinksLid() {
        String expected = """
                {"atomic:operations":[{"op":"add","data":{"type":"article","lid":"13","attributes":{"name":"article"},"relationships":{"post":{"links":{"self":"https://elide.io/group/com.example.repository2/relationships/products","related":"https://elide.io/group/com.example.repository2/products"},"data":null}}}}]}""";
        String actual = JsonApiDSL.atomicOperations(
                atomicOperation(AtomicOperationCode.add,
                        datum(resource(
                                type("article"),
                                lid("13"),
                                attributes(
                                        attr("name", "article")
                                ),
                                relationships(relation("post", true,
                                        links(
                                        attr("self",
                                                "https://elide.io/"
                                                        + "group/com.example.repository2/relationships/products"),
                                        attr("related", "https://elide.io/" + "group/com.example.repository2/products"))

                                )))))
                ).toJSON();
        assertEquals(expected, actual);
    }

    @Test
    void verifyAtomicOperationDataAttributesRelationshipsLinksResourceLinkageLid() {
        String expected = """
                {"atomic:operations":[{"op":"add","data":{"type":"article","lid":"13","attributes":{"name":"article"},"relationships":{"post":{"links":{"self":"https://elide.io/group/com.example.repository2/relationships/products","related":"https://elide.io/group/com.example.repository2/products"},"data":{"type":"group","id":"2"}}}}}]}""";
        String actual = JsonApiDSL.atomicOperations(
                atomicOperation(AtomicOperationCode.add,
                        datum(resource(
                                type("article"),
                                lid("13"),
                                attributes(
                                        attr("name", "article")
                                ),
                                relationships(relation("post", true,
                                        links(
                                        attr("self",
                                                "https://elide.io/"
                                                        + "group/com.example.repository2/relationships/products"),
                                        attr("related", "https://elide.io/" + "group/com.example.repository2/products")),
                                        linkage(type("group"), id("2"))

                                )))))
                ).toJSON();
        assertEquals(expected, actual);
    }

    @Test
    void verifyAtomicOperationRefId() {
        String expected = """
                {"atomic:operations":[{"op":"remove","ref":{"type":"articles","id":"13"}}]}""";
        String actual = JsonApiDSL.atomicOperations(
                    JsonApiDSL.atomicOperation(AtomicOperationCode.remove,
                            JsonApiDSL.ref(JsonApiDSL.type("articles"), JsonApiDSL.id("13"))
                    )
                ).toJSON();
        assertEquals(expected, actual);
    }

    @Test
    void verifyAtomicOperationRefLid() {
        String expected = """
                {"atomic:operations":[{"op":"remove","ref":{"type":"articles","lid":"13"}}]}""";
        String actual = JsonApiDSL.atomicOperations(
                    JsonApiDSL.atomicOperation(AtomicOperationCode.remove,
                            JsonApiDSL.ref(JsonApiDSL.type("articles"), JsonApiDSL.lid("13"))
                    )
                ).toJSON();
        assertEquals(expected, actual);
    }

    @Test
    void verifyAtomicOperationRefIdRelationship() {
        String expected = """
                {"atomic:operations":[{"op":"update","ref":{"type":"articles","id":"13","relationship":"author"},"data":null}]}""";
        String actual = JsonApiDSL.atomicOperations(
                    JsonApiDSL.atomicOperation(AtomicOperationCode.update,
                            JsonApiDSL.ref(JsonApiDSL.type("articles"), JsonApiDSL.id("13"), JsonApiDSL.relationship("author")),
                            JsonApiDSL.datum(null)
                    )
                ).toJSON();
        assertEquals(expected, actual);
    }

    @Test
    void verifyAtomicOperationRefLidRelationship() {
        String expected = """
                {"atomic:operations":[{"op":"update","ref":{"type":"articles","lid":"13","relationship":"author"},"data":null}]}""";
        String actual = JsonApiDSL.atomicOperations(
                    JsonApiDSL.atomicOperation(AtomicOperationCode.update,
                            JsonApiDSL.ref(JsonApiDSL.type("articles"), JsonApiDSL.lid("13"), JsonApiDSL.relationship("author")),
                            JsonApiDSL.datum(null)
                    )
                ).toJSON();
        assertEquals(expected, actual);
    }
}
