package com.yahoo.elide.datastores.inmemory;

import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.initialization.IntegrationTest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class MetaIT extends IntegrationTest {
    @Override
    protected DataStoreTestHarness createHarness() {
        return new WithMetaInMemoryDataStoreHarness();
    }

    @Test
    public void testCreateAndFetch() {
        given().when().get("/widget").then().log().all().statusCode(HttpStatus.SC_OK);
    }
}
