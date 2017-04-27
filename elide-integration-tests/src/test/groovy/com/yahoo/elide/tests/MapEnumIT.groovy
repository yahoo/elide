/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.yahoo.elide.core.HttpStatus
import com.yahoo.elide.initialization.AbstractIntegrationTestInitializer
import org.testng.Assert
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given

/**
 * Test rehydration of Map of Enums.
 */
class MapEnumIT extends AbstractIntegrationTestInitializer {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testPostColorShape() {
        // Create MapColorShape using Post
        def postRequest = given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
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
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
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
                .accept(JSONAPI_CONTENT_TYPE)
                .get("/mapColorShape/${postResponse['data']['id'].asText()}/")
        def getResponse = mapper.readTree(getRequest.asString())
        Assert.assertEquals(getResponse["data"]["attributes"]["colorShapeMap"]["Blue"].asText(), "Square")
    }

    @Test
    public void testPatchExtensionColorShape() {
        // Create MapColorShape using Patch extension
        def request = given()
                .contentType(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
                .accept(JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION)
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
