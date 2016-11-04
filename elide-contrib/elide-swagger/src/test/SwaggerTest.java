package test;

import junit.framework.*;

import com.yahoo.elide.contrib.swagger.*;

public class SwaggerTest extends TestCase {
    public void testRequired()
    {
        Swagger s = new Swagger();
        assertFalse(s.checkRequired());
        s.info = new Info();
        assertFalse(s.checkRequired());
        s.paths = new Paths();
        assertTrue(s.checkRequired());
    }
    public void testCheckAllRequired()
    {
        Swagger s = new Swagger();
        assertFalse(Swagger.checkAllRequired(s));
        s.info = new Info();
        s.paths = new Paths();
        assertFalse(Swagger.checkAllRequired(s));
        s.info.title = "Title";
        s.info.version = "15.52.1.5";
        assertTrue(Swagger.checkAllRequired(s));
    }
}
