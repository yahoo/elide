/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests

import static com.jayway.restassured.RestAssured.given

import com.yahoo.elide.core.HttpStatus
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer

import com.fasterxml.jackson.databind.ObjectMapper

import org.testng.Assert
import org.testng.annotations.Test

/**
 * Test rehydration of Map of Enums.
 */
class MapEnumIT extends AbstractIntegrationTestInitializer {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testPostColorShape() {
        // Create MapColorShape using Post
        def postRequest = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body('''
                    {
                      "data": {
                        "type": "mapColorShape",
                        "attributes": {
                          "colorShapeMap": {
                            "Red": "Circle"
                          }
                        }
                      }
                    }
                    ''')
                .post("/mapColorShape")
        postRequest.then().statusCode(HttpStatus.SC_CREATED)

        def postResponse = mapper.readTree(postRequest.asString())
        Assert.assertEquals(postResponse["data"]["attributes"]["colorShapeMap"]["Red"].asText(), "Circle")

        // Update MapColorShape using Patch
        def patchRequest = given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body("""
                      {
                        "data": {
                          "id": ${postResponse['data']['id']},
                          "type": "mapColorShape",
                          "attributes": {
                            "colorShapeMap": {
                              "Blue": "Square"
                            }
                          }
                        }
                      }
                    ]
                    """)
                .patch("/mapColorShape/${postResponse['data']['id'].asText()}/")
        patchRequest.then().statusCode(HttpStatus.SC_NO_CONTENT)

        // Get MapColorShape
        def getRequest = given()
                .accept("application/vnd.api+json")
                .get("/mapColorShape/${postResponse['data']['id'].asText()}/")
        def getResponse = mapper.readTree(getRequest.asString())
        Assert.assertEquals(getResponse["data"]["attributes"]["colorShapeMap"]["Blue"].asText(), "Square")
    }

    @Test
    public void testPatchExtensionColorShape() {
        // Create MapColorShape using Patch extension
        def request = given()
                .contentType("application/vnd.api+json; ext=jsonpatch")
                .accept("application/vnd.api+json; ext=jsonpatch")
                .body('''
                    [
                      {
                        "op": "add",
                        "path": "/mapColorShape",
                        "value": {
                          "id": "12345681-1234-1234-1234-1234567890ab",
                          "type": "mapColorShape",
                          "attributes": {
                            "colorShapeMap": {
                              "Blue": "Triangle"
                            }
                          }
                        }
                      }
                    ]
                    ''')
                .patch("/")
        request.then().statusCode(HttpStatus.SC_OK)

        def response = mapper.readTree(request.asString())
        Assert.assertEquals(response[0]["data"]["attributes"]["colorShapeMap"]["Blue"].asText(), "Triangle")
    }
}
