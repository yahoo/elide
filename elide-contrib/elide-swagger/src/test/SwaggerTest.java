package test;

import junit.framework.*;

import com.yahoo.elide.contrib.swagger.JSONObjectClasses.*;

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
    /*
     * The point of this method is to make sure
     * that the checkAllRequired() method goes all the way down the stack
     */
    public void testCheckAllRequired() throws SwaggerValidationException
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
        catch (SwaggerValidationException e)
        {
        }
        s.info.title = "Title";
        s.info.version = "15.52.1.5";
        Swagger.checkAllRequired(s);
        s.info.contact = new Contact();
        Swagger.checkAllRequired(s);
        s.info.contact.name = "Dewey Finn";
        s.info.contact.url = "the internet";
        try {
            Swagger.checkAllRequired(s);
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {
        }
        s.info.contact.url = "http://xkcd.com";
        Swagger.checkAllRequired(s);
        s.info.contact.email = "1600 Pennsylvania Avenue";
        try {
            Swagger.checkAllRequired(s);
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {
        }
        s.info.contact.email = "fred@television.tv";
        Swagger.checkAllRequired(s);


    }
    public void testPathsRejectsBadPath() {
        Paths paths = new Paths();
        try {
            paths.put("test", new Path());
            fail("Something isn't working right; there should be an exception here");
        }
        catch (IllegalArgumentException e)
        {}
    }

    public void testPathValidator() throws SwaggerValidationException
    {
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
    }

    public void testParameterValidator() throws SwaggerValidationException
    {
        Parameter p = new Parameter();
        try {
            p.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        p.name = "This is a test name";
        try {
            p.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        p.in = Enums.Location.BODY;
        try {
            p.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
    }

    /*
     * I'm going to test all the validation methods on the classes where they're 
     * overridden.
     */
    public void testContactsValidation() throws SwaggerValidationException
    {
        Contact contact = new Contact();
        contact.checkRequired();
        contact.url = "Hey boys! How's the water?";
        try {
            contact.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        contact.url = "http://i.imgur.com/DzrJ904.gifv";
        contact.checkRequired();
        contact.email = "http://i.imgur.com/dYz2tCE.gifv";
        try {
            contact.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        contact.email = "ned@horacegreen.edu";
        contact.checkRequired();
    }

    public void testHeaderValidation() throws SwaggerValidationException 
    {
        Header header = new Header();
        try {
            header.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        header.type = Enums.Type.STRING;
        header.checkRequired();
        header.type = Enums.Type.ARRAY;
        try {
            header.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        header.items = new Items();
        header.checkRequired();
        // This should fail because items won't validate. I'll check this 
        // again later, but this is just to make extra sure. 
        try {
            Swagger.checkAllRequired(header);
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
    }

    public void testItemsValidation() throws SwaggerValidationException
    {
        Items items = new Items();
        try {
            items.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}

        items.type = Enums.Type.STRING;
        items.checkRequired();
        items.type = Enums.Type.ARRAY;
        try {
            items.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        items.items = new Items();
        items.checkRequired();

        try {
            Swagger.checkAllRequired(items);
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
    }

    public void testOperationRejectsDuplicateId() throws SwaggerValidationException
    {
        Operation operation = new Operation();
        operation.setOperationId("Cache invalidation");
        Operation other = new Operation();
        other.setOperationId("Naming things");
        Operation otherOther = new Operation();
        try {
            otherOther.setOperationId("Cache invalidation");
            fail("Something isn't working right; there should be an exception here");
        }
        catch (IllegalArgumentException e)
        {}
        operation.setOperationId("Ordered network requests");
        otherOther.setOperationId("Cache invalidation");
    }

    public void testOperationValidation() throws SwaggerValidationException
    {
        Operation operation = new Operation();
        operation.responses = new Responses();

        Parameter[] params = new Parameter[2];
        params[0] = new Parameter();
        params[0].in = Enums.Location.BODY;
        params[0].name = "Test";

        params[1] = new Parameter();
        params[1].in = Enums.Location.BODY;
        params[1].name = "Test";
        operation.parameters = params;
        try {
            operation.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        params[1] = new Parameter();
        params[1].in = Enums.Location.BODY;
        params[1].name = "Other test";
        try {
            operation.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        params[1].in = Enums.Location.FORM_DATA;
        operation.checkRequired();
        params[0].type = Enums.Type.FILE;
        try {
            operation.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        operation.consumes = new MimeType[] {new MimeType("multipart/form-data")};
        operation.checkRequired();
    }

    public void testSchemaValidation() throws SwaggerValidationException 
    {
        Schema schema = new Schema();
        schema.ref = "Is this water?";
        try {
            schema.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}
        schema.ref = "https://fat.gfycat.com/EnlightenedSpitefulBlackwidowspider.gif";
        schema.checkRequired();
    }

    public void testSecurityRequirementValidation() throws SwaggerValidationException
    {
        SecurityRequirement secReq = new SecurityRequirement();
        secReq.checkRequired();
        // Check for something other than oauth...
        secReq.put("A secret handshake", new String[] {"butterfly", "spin around", "sign the alphabet"});
        try {
            secReq.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}

        // ... a null oauth array...
        secReq.put("oauth2", null);
        try {
            secReq.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}

        // ... an empty oauth array...
        secReq.put("oauth2", new String[0]);
        try {
            secReq.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}

        // ... an oauth array and something else (the secret handshake is still 
        // there, remember)
        secReq.put("oauth2", new String[] {"An understanding of web security"});
        try {
            secReq.checkRequired();
            fail("Something isn't working right; there should be an exception here");
        }
        catch (SwaggerValidationException e)
        {}

        secReq.remove("A secret handshake");

        // ... and a "proper" oauth
        secReq.checkRequired();
    }
}
