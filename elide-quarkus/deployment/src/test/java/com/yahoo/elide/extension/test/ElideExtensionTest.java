package com.yahoo.elide.extension.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yahoo.elide.Elide;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.extension.test.models.Book;
import com.yahoo.elide.jsonapi.resources.JsonApiEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

import javax.inject.Inject;

public class ElideExtensionTest {

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
        .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                .addAsResource("application.properties")
                .addClass(Book.class));

    @Inject
    EntityDictionary dictionary;

    @Inject
    Elide elide;

    @Inject
    Injector injector;

    @Test
    public void testBookEndpoint() {
        RestAssured.when().get("/jsonapi/book").then().log().all().statusCode(200);
    }

    @Test
    public void testSwaggerEndpoint() {
        RestAssured.when().get("/doc").then().log().all().statusCode(200);
    }

    @Test
    public void testSwaggerApiEndpoint() {
        RestAssured.when().get("/doc/api").then().log().all().statusCode(200);
    }

    @Test
    public void testInjection() {

        EntityDictionary dictionary = injector.instantiate(EntityDictionary.class);
        assertNotNull(dictionary);

        Book test = injector.instantiate(Book.class);
        assertNotNull(test);
    }
}
