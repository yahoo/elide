package test;

import junit.framework.*;

import com.yahoo.elide.contrib.swagger.*;

public class SwaggerTest extends TestCase {
    public void testTests()
    {
        Swagger swagger = new Swagger();
        assertTrue(swagger.swagger.equals("2.0"));
    }
}
