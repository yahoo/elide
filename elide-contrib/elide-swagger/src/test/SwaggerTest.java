package test;

import junit.framework.*;

import com.yahoo.elide.contrib.swagger.*;

public class SwaggerTest extends TestCase {
    public void testRequired()
    {
        Swagger s = new Swagger();
        try {
            s.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (RuntimeException e)
        {}
        s.info = new Info();
        try {
            s.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (RuntimeException e)
        {
        }
        s.paths = new Paths();
        s.checkRequired();
    }
    public void testCheckAllRequired()
    {
        Swagger s = new Swagger();
        try {
            Swagger.checkAllRequired(s);
            fail("Something isn't working right; there should be an exception here");
        }
        catch (RuntimeException e)
        {
        }
        s.info = new Info();
        s.paths = new Paths();
        try {
            Swagger.checkAllRequired(s);
            fail("Something isn't working right; there should be an exception here");
        }
        catch (RuntimeException e)
        {
        }
        s.info.title = "Title";
        s.info.version = "15.52.1.5";
        Swagger.checkAllRequired(s);
    }
}
