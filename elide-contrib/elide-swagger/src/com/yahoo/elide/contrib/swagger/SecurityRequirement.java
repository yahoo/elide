package com.yahoo.elide.contrib.swagger;
import java.util.HashMap;
import java.util.Map.Entry;

public class SecurityRequirement extends HashMap<String, String[]> implements Requirer {
    @Override
    public void checkRequired()
    {
        for(Entry<String, String[]> e : this.entrySet())
        {
            if(e.getKey().equals("oauth2"))
            {
                if(e.getValue().length == 0)
                    throw new RuntimeException("You have to have a non-zero length array in the oauth security requirement");
            }
            else
                if(e.getValue().length != 0)
                    throw new RuntimeException("Security requirements for anything other than oauth2 are undefined");
        }
    }
}
