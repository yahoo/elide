/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.tests;

import static com.paiondata.elide.test.graphql.GraphQLDSL.argument;
import static com.paiondata.elide.test.graphql.GraphQLDSL.arguments;
import static com.paiondata.elide.test.graphql.GraphQLDSL.document;
import static com.paiondata.elide.test.graphql.GraphQLDSL.field;
import static com.paiondata.elide.test.graphql.GraphQLDSL.mutation;
import static com.paiondata.elide.test.graphql.GraphQLDSL.selection;
import static com.paiondata.elide.test.graphql.GraphQLDSL.selections;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attr;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.data;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.datum;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.id;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.linkage;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relation;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.relationships;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.resource;
import static com.paiondata.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;
import com.paiondata.elide.initialization.GraphQLIntegrationTest;
import com.paiondata.elide.jsonapi.JsonApi;
import com.google.common.collect.Sets;
import example.embeddedid.Address;
import example.embeddedid.AddressSerde;
import example.embeddedid.Building;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.Data;

import java.io.IOException;
import java.util.Arrays;

public class EmbeddedIdIT extends GraphQLIntegrationTest {

    protected Address address1 = new Address(0, "Bullion Blvd", 40121);
    protected Address address2 = new Address(1409, "W Green St", 61801);
    protected Address address3 = new Address(1800, "South First Street", 61820);
    protected AddressSerde serde = new AddressSerde();

    @BeforeAll
    public void beforeAll() {
        CoerceUtil.register(Address.class, serde);
    }

    @BeforeEach
    public void setup() throws IOException {
        dataStore.populateEntityDictionary(EntityDictionary.builder().build());
        DataStoreTransaction tx = dataStore.beginTransaction();

        Building building1 = new Building();
        building1.setAddress(address1);
        building1.setName("Fort Knox");

        Building building2 = new Building();
        building2.setAddress(address3);
        building2.setName("Assembly Hall");

        building1.setNeighbors(Sets.newHashSet(building2));
        building2.setNeighbors(Sets.newHashSet(building1));

        tx.createObject(building1, null);
        tx.createObject(building2, null);

        tx.commit(null);
        tx.close();
    }

    @Test
    public void testJsonApiFetchCollection() {
        String address1Id = serde.serialize(address1);
        String address3Id = serde.serialize(address3);

        given()
                .when()
                .get("/building")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        data(
                                resource(
                                        type("building"),
                                        id(address1Id),
                                        attributes(
                                                attr("name", "Fort Knox")
                                        ),
                                        relationships(
                                                relation("neighbors",
                                                        linkage(type("building"), id(address3Id))
                                                )
                                        )
                                ),
                                resource(
                                        type("building"),
                                        id(address3Id),
                                        attributes(
                                                attr("name", "Assembly Hall")
                                        ),
                                        relationships(
                                                relation("neighbors",
                                                        linkage(type("building"), id(address1Id))
                                                )
                                        )
                                )
                        ).toJSON())
                );
    }

    @Test
    public void testJsonApiFetchById() {
        String address1Id = serde.serialize(address1);
        String address3Id = serde.serialize(address3);

        given()
                .when()
                .get("/building/" + address1Id)
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                    datum(
                        resource(
                                type("building"),
                                id(address1Id),
                                attributes(
                                        attr("name", "Fort Knox")
                                ),
                                relationships(
                                        relation("neighbors",
                                                linkage(type("building"), id(address3Id))
                                        )
                                )
                        )
                ).toJSON())
        );
    }

    @Test
    public void testJsonApiFetchRelationship() {
        String address1Id = serde.serialize(address1);
        String address3Id = serde.serialize(address3);

        given()
                .when()
                .get("/building/" + address1Id + "/neighbors")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                        data(
                                resource(
                                        type("building"),
                                        id(address3Id),
                                        attributes(
                                                attr("name", "Assembly Hall")
                                        ),
                                        relationships(
                                                relation("neighbors",
                                                        linkage(type("building"), id(address1Id))
                                                )
                                        )
                                )
                        ).toJSON())
                );
    }

    @Test
    public void testJsonApiFilterById() {
        String address1Id = serde.serialize(address1);
        String address3Id = serde.serialize(address3);

        given()
            .when()
            .get("/building?filter=id=='" + address1Id + "'")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo(
                    data(
                        resource(
                            type("building"),
                            id(address1Id),
                            attributes(
                                attr("name", "Fort Knox")
                            ),
                            relationships(
                                relation("neighbors",
                                linkage(type("building"), id(address3Id))
                            )
                        )
                    )
                ).toJSON()
            ));
    }

    @Test
    public void testJsonApiCreate() {
        given()
                .contentType(JsonApi.MEDIA_TYPE)
                .accept(JsonApi.MEDIA_TYPE)
                .body(data(
                        resource(
                                type("building"),
                                id(serde.serialize(address2)),
                                attributes(attr(
                                        "name", "Altgeld Hall"
                                ))
                        )

                ))
                .when()
                .post("/building")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body(equalTo(datum(
                        resource(
                            type("building"),
                            id(serde.serialize(address2)),
                            attributes(attr(
                                    "name", "Altgeld Hall"
                            )),
                            relationships(relation("neighbors"))
                        )).toJSON()
                ));
    }


    @Test
    public void testGraphQLFetchCollection() throws Exception {
        String graphQLRequest = document(
                selection(
                        field(
                                "building",
                                selections(
                                        field("address"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "building",
                                selections(
                                        field("address", serde.serialize(address1)),
                                        field("name", "Fort Knox")
                                ),
                                selections(
                                        field("address", serde.serialize(address3)),
                                        field("name", "Assembly Hall")
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLFetchById() throws Exception {
        String addressId = serde.serialize(address1);

        String graphQLRequest = document(
                selection(
                        field(
                                "building",
                                arguments(
                                        argument("ids", Arrays.asList(addressId))
                                ),
                                selections(
                                        field("address"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "building",
                                selections(
                                        field("address", addressId),
                                        field("name", "Fort Knox")
                                )

                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLFetchRelationship() throws Exception {
        String address1Id = serde.serialize(address1);
        String address3Id = serde.serialize(address3);

        String graphQLRequest = document(
                selection(
                        field(
                                "building",
                                arguments(
                                        argument("ids", Arrays.asList(address1Id))
                                ),
                                selections(
                                        field("neighbors",
                                                selections(
                                                        field("name"),
                                                        field("address")
                                                )
                                        )
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "building",
                                selections(
                                        field("neighbors",
                                                selections(
                                                        field("name", "Assembly Hall"),
                                                        field("address", address3Id)
                                                )
                                        )
                                )
                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLFilterById() throws Exception {
        String addressId = serde.serialize(address1);

        String graphQLRequest = document(
                selection(
                        field(
                                "building",
                                arguments(
                                        argument("filter", "\"id==\\\"" + addressId + "\\\"\"")
                                ),
                                selections(
                                        field("address"),
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "building",
                                selections(
                                        field("address", addressId),
                                        field("name", "Fort Knox")
                                )

                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }

    @Test
    public void testGraphQLCreate() throws Exception {

        @Data
        class SerializedBuilding {
            private String name;
            private String address;
        }

        String addressId = serde.serialize(address2);
        SerializedBuilding building = new SerializedBuilding();
        building.address = addressId;
        building.name = "Altgeld Hall";

        String graphQLRequest = document(
            mutation(
                    selection(
                            field(
                                    "building",
                                    arguments(
                                            argument("op", "UPSERT"),
                                            argument("data", building)
                                    ),
                                    selections(
                                            field("address"),
                                            field("name")
                                    )
                            )
                    )
            )
        ).toQuery();

        String expected = document(
                selections(
                        field(
                                "building",
                                selections(
                                        field("address", addressId),
                                        field("name", "Altgeld Hall")
                                )

                        )
                )
        ).toResponse();

        runQueryWithExpectedResult(graphQLRequest, expected);
    }
}
