package test;

import junit.framework.*;

import com.yahoo.elide.contrib.swagger.*;

public class SwaggerTest extends TestCase {

    private Swagger getFunctionalSwagger()
    {
        Swagger retval = new Swagger();
        retval.info = new Info();
        retval.paths = new Paths();
        return retval;
    }

    public void testRequired() throws SwaggerValidationException
    {
        Swagger s = new Swagger();
        try {
            s.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        s.info = new Info();
        try {
            s.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
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
        catch (SwaggerValidationException e)
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

        Path dummyPath = new Path();
        dummyPath.checkRequired();
        dummyPath.ref = "apples";
        try {
            dummyPath.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {
        }
        dummyPath.ref = "http://i.imgur.com/foWGjVK.gifv";
        dummyPath.checkRequired();
        dummyPath.parameters = new Parameter[1];
        dummyPath.parameters[0] = new Parameter();
        dummyPath.parameters[0].name = "This is the name of a parameter";
        dummyPath.parameters[0].in = Enums.Location.BODY;
        dummyPath.checkRequired();

        dummyPath.parameters = new Parameter[2];

        dummyPath.parameters[0] = new Parameter();
        dummyPath.parameters[0].name = "This is the name of a parameter";
        dummyPath.parameters[0].in = Enums.Location.BODY;

        dummyPath.parameters[1] = new Parameter();
        dummyPath.parameters[1].name = "This is the name of a parameter";
        dummyPath.parameters[1].in = Enums.Location.BODY;
        try {
            dummyPath.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        s.paths.put("/test", dummyPath);
    }
    public void testPathsRejectsBadPath() {
        // TODO: Make sure that if you try to add a path to a paths that doesn't have the proper
        // structure to its key (ie, it doesn't start with a slash), then the computer should 
        // get mad.
    }
}
