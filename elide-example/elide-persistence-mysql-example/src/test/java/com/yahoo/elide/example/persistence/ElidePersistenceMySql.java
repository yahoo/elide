package com.yahoo.elide.example.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.yahoo.elide.resources.JsonApiEndpoint;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Elide persistence MySQL integration test
 */
public class ElidePersistenceMySql {
    private Server server;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Read resource as a JSON string.
     *
     * @param  resourceName name of the desired resource
     * @return JSON string
     */
    public String getJson(String resourceName) {
        try (InputStream is = ElidePersistenceMySql.class.getResourceAsStream(resourceName)) {
            return String.valueOf(mapper.readTree(is));
        } catch (IOException e) {
            Assert.fail("Unable to open test data " + resourceName, e);
            throw new IllegalStateException(); // should not reach here
        }
    }

    @BeforeClass
    public void setup() throws Exception {
        RestAssured.baseURI = "http://localhost/";
        RestAssured.basePath = "/";
        String restassuredPort = System.getProperty("restassured.port", System.getenv("restassured.port"));
        RestAssured.port = Integer.parseInt(restassuredPort != null && !restassuredPort.isEmpty() ? restassuredPort : "9999");

        server = new Server(RestAssured.port);
        final ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/");
        server.setHandler(servletContextHandler);

        final ServletHolder servletHolder = servletContextHandler.addServlet(ServletContainer.class, "/*");
        servletHolder.setInitOrder(1);
        servletHolder.setInitParameter("jersey.config.server.provider.packages",
                JsonApiEndpoint.class.getPackage().getName());
        servletHolder.setInitParameter("javax.ws.rs.Application",
                ElideResourceConfig.class.getCanonicalName());

        server.start();

    }

    @Test
    public void verifyAll() throws IOException {
        // Create Author: Ernest Hemingway
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/ernest_hemingway.json"))
                .post("/author");

        // Create Book: The Old Man and the Sea
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/the_old_man_and_the_sea.json"))
                .post("/book");

        // Create Relationship: Ernest Hemingway -> The Old Man and the Sea
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/ernest_hemingway_relationship.json"))
                .patch("/book/1/relationships/authors");

        // Create Author: Orson Scott Card
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/orson_scott_card.json"))
                .post("/author");

        // Create Book: Ender's Game
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/enders_game.json"))
                .post("/book");

        // Create Relationship: Orson Scott Card -> Ender's Game
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/orson_scott_card_relationship.json"))
                .patch("/book/2/relationships/authors");

        // Create Book: For Whom the Bell Tolls
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/for_whom_the_bell_tolls.json"))
                .post("/book");

        // Create Relationship: Ernest Hemingway -> For Whom the Bell Tolls
        RestAssured
                .given()
                .contentType("application/vnd.api+json")
                .accept("application/vnd.api+json")
                .body(getJson("/ernest_hemingway_relationship.json"))
                .patch("/book/3/relationships/authors");

        // Verify that we get back 3 books with the correct authors
        JsonNode result = mapper.readTree(
                RestAssured
                        .given()
                        .contentType("application/vnd.api+json")
                        .accept("application/vnd.api+json")
                        .get("/book/3").asString());

        Assert.assertEquals(
                result.get("data").get("attributes").get("title").asText(),
                "For Whom the Bell Tolls"
        );

        Assert.assertEquals(
                result.get("data").get("relationships").get("authors").get("data").get(0).get("id").asInt(),
                1
        );
    }

    @AfterClass
    public void teardown() {
        try {
            server.stop();
            server.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
