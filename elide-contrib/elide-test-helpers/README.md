# Elide Test Helpers

A set of helpers for testing Elide web services

## Installation
```xml
<dependency>
  <groupId>com.yahoo.elide</groupId>
  <artifactId>elide-test-helpers</artifactId>
  <version>${elide.verison}</version>
  <scope>test</scope>
</dependency>
```

## Usage

The `JsonApiDSL` can be used to build JSON:API documents in a typesafe way.

```java
package com.yahoo.elideinstnace.integration

import org.apache.http.HttpStatus;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.*;

public class ReviewTest {
  @Test
  public void createReview() {

    given().
    contentType("application/vnd.api+json").
    body(
      data(
        resource(
          type("review"),
          attributes(
            attr("title", "My Awesome Review")
          ),
          relationships(
          	relation("author",
              linkage(
                type("author"), 
                id("1")
              )
            )
          )
        )
      ).toJSON()
    ).
    when().
      post("/reviews/").
    then().
      statusCode(HttpStatus.SC_CREATED).
      body(
        "data.id", equalTo("22"),
      );
  }
}
