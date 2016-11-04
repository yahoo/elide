package com.yahoo.elide.contrib.swagger;
import java.util.HashMap;
import java.util.Map.Entry;

public class SecurityRequirement extends HashMap<String, String[]> implements Requirer {
    public boolean checkRequired()
    {
        for(Entry<String, String[]> e : this)
        {
            if(e.getKey().equals("oauth2"))
            {
                if(e.getValue().length == 0)
                    return false;
            }
            else
                if(e.getValue().length != 0)
                    return false;
        }
        return true;
    }
}
