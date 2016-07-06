/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests
import com.fasterxml.jackson.databind.ObjectMapper
import com.yahoo.elide.core.DataStoreTransaction
import com.yahoo.elide.core.HttpStatus
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer
import example.Left
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given
/**
 * @Shareable annotation integration tests
 */
class ShareableIT extends AbstractIntegrationTestInitializer {
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public void setUp() {
        DataStoreTransaction tx = dataStore.beginTransaction();
        Left left = new Left();
        tx.save(left);
        tx.commit();
    }

    @Test
    public void testUnshareableForbiddenAccess() {
        // Create container
        def container = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body('''
                    {
                      "data": {
                        "type": "container"
                      }
                    }''')
                .post("/container")
        container.then().statusCode(HttpStatus.SC_CREATED)
        def containerJson = mapper.readTree(container.asString())

        // Create unshareable
        def unshareable = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body('''
                    {
                      "data": {
                        "type": "unshareable"
                      }
                    }
                    ''')
                .post("/unshareable")
        unshareable.then().statusCode(HttpStatus.SC_CREATED)
        def unshareableJson = mapper.readTree(unshareable.asString())

        // Fail to add unshareable to container's unshareables (unshareable is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body("""
                    {
                      "data": {
                        "type": "unshareable",
                        "id": ${unshareableJson['data']['id']}
                      }
                    }
                    """)
                .patch("/container/${containerJson['data']['id'].asText()}/relationships/unshareables")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)

        // Fail to replace container's unshareables collection (unshareable is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body("""
                    {
                      "data": {
                        "type": "unshareable",
                        "id": ${unshareableJson['data']['id']}
                      }
                    }
                    """)
                .post("/container/${containerJson['data']['id'].asText()}/relationships/unshareables")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)

        // Fail to update unshareable's container (container is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body("""
                    {
                      "data": {
                        "type": "container",
                        "id": ${containerJson['data']['id']}
                      }
                    }
                    """)
                .patch("/unshareable/${unshareableJson['data']['id'].asText()}/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)

        // Fail to set unshareable's container (container is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body("""
                    {
                      "data": {
                        "type": "container",
                        "id": ${containerJson['data']['id']}
                      }
                    }
                    """)
                .post("/unshareable/${unshareableJson['data']['id'].asText()}/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
    }

    @Test
    public void testShareableForbiddenAccess() {
        // Create container
        def container = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body('''
                    {
                      "data": {
                        "type": "container"
                      }
                    }''')
                .post("/container")
        container.then().statusCode(HttpStatus.SC_CREATED)
        def containerJson = mapper.readTree(container.asString())

        // Create shareable
        def shareable = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body('''
                    {
                      "data": {
                        "type": "shareable"
                      }
                    }
                    ''')
                .post("/shareable")
        shareable.then().statusCode(HttpStatus.SC_CREATED)
        def shareableJson = mapper.readTree(shareable.asString())

        // Fail to update shareable's container (container is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body("""
                    {
                      "data": {
                        "type": "container",
                        "id": ${containerJson['data']['id']}
                      }
                    }
                    """)
                .patch("/shareable/${shareableJson['data']['id'].asText()}/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)

        // Fail to set shareable's container (container is not shareable)
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body("""
                    {
                      "data": {
                        "type": "container",
                        "id": ${containerJson['data']['id']}
                      }
                    }
                    """)
                .post("/shareable/${shareableJson['data']['id'].asText()}/relationships/container")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
    }

    @Test
    public void testShareablePost() {
        // Create container
        def container = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body('''
                    {
                      "data": {
                        "type": "container"
                      }
                    }''')
                .post("/container")
        container.then().statusCode(HttpStatus.SC_CREATED)
        def containerJson = mapper.readTree(container.asString())

        // Create shareable
        def shareable = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body('''
                    {
                      "data": {
                        "type": "shareable"
                      }
                    }
                    ''')
                .post("/shareable")
        shareable.then().statusCode(HttpStatus.SC_CREATED)
        def shareableJson = mapper.readTree(shareable.asString())

        // Add shareable to container's shareables
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body("""
                    {
                      "data": {
                        "type": "shareable",
                        "id": ${shareableJson['data']['id']}
                      }
                    }
                    """)
                .post("/container/${containerJson['data']['id'].asText()}/relationships/shareables")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)

        def result = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/container/${containerJson['data']['id'].asText()}")
        result.then().statusCode(HttpStatus.SC_OK)

        def resultJson = mapper.readTree(result.asString())
        Assert.assertEquals(resultJson['data']['relationships']['shareables'].size(), 1)
    }

    @Test
    public void testShareablePatch() {
        // Create container
        def container = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body('''
                    {
                      "data": {
                        "type": "container"
                      }
                    }''')
                .post("/container")
        container.then().statusCode(HttpStatus.SC_CREATED)
        def containerJson = mapper.readTree(container.asString())

        // Create shareable
        def shareable = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body('''
                    {
                      "data": {
                        "type": "shareable"
                      }
                    }
                    ''')
                .post("/shareable")
        shareable.then().statusCode(HttpStatus.SC_CREATED)
        def shareableJson = mapper.readTree(shareable.asString())

        // Add shareable to container's shareables
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body("""
                    {
                      "data": {
                        "type": "shareable",
                        "id": ${shareableJson['data']['id']}
                      }
                    }
                    """)
                .patch("/container/${containerJson['data']['id'].asText()}/relationships/shareables")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)

        def result = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .get("/container/${containerJson['data']['id'].asText()}")
        result.then().statusCode(HttpStatus.SC_OK)

        def resultJson = mapper.readTree(result.asString())
        Assert.assertEquals(resultJson['data']['relationships']['shareables'].size(), 1)
    }

    @Test
    public void testCreateContainerAndUnshareables() {
        def patchResponse = given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body("""
                    [
                      {
                        "op": "add",
                        "path": "/container",
                        "value": {
                          "id": "12345678-1234-1234-1234-1234567890ab",
                          "type": "container",
                          "relationships": {
                            "unshareables": {
                              "data": [
                                {
                                  "type": "unshareable",
                                  "id": "12345678-1234-1234-1234-1234567890ac"
                                },
                                {
                                  "type": "unshareable",
                                  "id": "12345678-1234-1234-1234-1234567890ad"
                                }
                              ]
                            }
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/unshareable",
                        "value": {
                          "type": "unshareable",
                          "id": "12345678-1234-1234-1234-1234567890ac"
                        }
                      },
                      {
                        "op": "add",
                        "path": "/unshareable",
                        "value": {
                          "type": "unshareable",
                          "id": "12345678-1234-1234-1234-1234567890ad"
                        }
                      }
                    ]
                    """)
                .patch("/")
        patchResponse.then().statusCode(HttpStatus.SC_OK)

        def patchJson = mapper.readTree(patchResponse.asString())

        // Should have 3 results, 1st is container, 2nd and 3rd are unshareables
        Assert.assertEquals(patchJson.size(), 3)
        Assert.assertEquals(patchJson[0]["data"]["type"].asText(), "container")
        Assert.assertEquals(patchJson[1]["data"]["type"].asText(), "unshareable")
        Assert.assertEquals(patchJson[2]["data"]["type"].asText(), "unshareable")

        // Container should have 2 unshareables
        Assert.assertEquals(patchJson[0]["data"]["relationships"]["unshareables"]["data"].size(), 2)
    }

    @Test
    public void testCreateContainerAndShareables() {
        def patchResponse = given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body("""
                    [
                      {
                        "op": "add",
                        "path": "/container",
                        "value": {
                          "id": "12345678-1234-1234-1234-1234567890ab",
                          "type": "container",
                          "relationships": {
                            "shareables": {
                              "data": [
                                {
                                  "type": "shareable",
                                  "id": "12345678-1234-1234-1234-1234567890ac"
                                },
                                {
                                  "type": "shareable",
                                  "id": "12345678-1234-1234-1234-1234567890ad"
                                }
                              ]
                            }
                          }
                        }
                      },
                      {
                        "op": "add",
                        "path": "/shareable",
                        "value": {
                          "type": "shareable",
                          "id": "12345678-1234-1234-1234-1234567890ac"
                        }
                      },
                      {
                        "op": "add",
                        "path": "/shareable",
                        "value": {
                          "type": "shareable",
                          "id": "12345678-1234-1234-1234-1234567890ad"
                        }
                      }
                    ]
                    """)
                .patch("/")
        patchResponse.then().statusCode(HttpStatus.SC_OK)

        def patchJson = mapper.readTree(patchResponse.asString())

        // Should have 3 results, 1st is container, 2nd and 3rd are shareables
        Assert.assertEquals(patchJson.size(), 3)
        Assert.assertEquals(patchJson[0]["data"]["type"].asText(), "container")
        Assert.assertEquals(patchJson[1]["data"]["type"].asText(), "shareable")
        Assert.assertEquals(patchJson[2]["data"]["type"].asText(), "shareable")

        // Container should have 2 shareables
        Assert.assertEquals(patchJson[0]["data"]["relationships"]["shareables"]["data"].size(), 2)
    }


    @Test(priority = 3)
    public void addUnsharedRelationship() {
        given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body("""
                    {
                        "data":{
                            "type":"right",
                            "relationships":{
                                "one2one":{
                                    "data":{
                                        "type":"left",
                                        "id":"1"
                                    }
                                }
                            }
                        }
                    }
                """)
                .post("/left/1/one2many")
                .then()
                .statusCode(HttpStatus.SC_CREATED);
    }
}
