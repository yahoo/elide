package com.yahoo.elide.contrib.testhelpers.relayjsonapi;

import static com.yahoo.elide.contrib.testhelpers.relayjsonapi.RelayJsonApiDSL.attribute;
import static com.yahoo.elide.contrib.testhelpers.relayjsonapi.RelayJsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.relayjsonapi.RelayJsonApiDSL.edges;
import static com.yahoo.elide.contrib.testhelpers.relayjsonapi.RelayJsonApiDSL.node;
import static com.yahoo.elide.contrib.testhelpers.relayjsonapi.RelayJsonApiDSL.resource;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RelayJsonApiTest {

    @Test
    public void testBasicResponse() {
        String expected = "{\"data\":" +
                "{\"book\":" +
                "{\"edges\":" +
                "[{\"node\":" +
                "{\"id\":\"1\"," +
                "\"title\":\"My first book\"," +
                "\"authors\":{\"edges\":[{\"node\":{" +
                "\"name\":\"Ricky Carmichael\"}}]}}}]}}}";

        String actual = datum(
                resource(
                        "book",
                        edges(
                                node(
                                        attribute("id", "1"),
                                        attribute("title", "My first book"),
                                        resource(
                                                "authors",
                                                edges(
                                                        node(
                                                                attribute("name", "Ricky Carmichael")

                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).toJSON();

        Assert.assertEquals(actual, expected);
    }
}
