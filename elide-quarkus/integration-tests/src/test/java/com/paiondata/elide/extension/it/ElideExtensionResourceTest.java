package com.paiondata.elide.extension.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ElideExtensionResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/elide-extension")
                .then()
                .statusCode(200)
                .body(is("Hello elide-extension"));
    }
}
