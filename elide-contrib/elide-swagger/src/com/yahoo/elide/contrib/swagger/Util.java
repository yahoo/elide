package com.yahoo.elide.contrib.swagger;

import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

class Util {
    public static boolean validateRef(String ref)
    {
        // The Internet says that ref has to be a URL. I don't understand what's supposed to be under that URL, but
        // I guess we can worry about that later.
        final Pattern URL = Pattern.compile("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)");
        Matcher matcher = URL.matcher(ref);
        if(!matcher.matches())
            return false;
        else
            return true;
    }
    public static boolean hasDuplicates(Object[] arr)
    {
        HashSet<Object> set = new HashSet<>();
        for(int i = 0; i < arr.length; i++)
        {
            if(set.contains(arr[i]))
                return true;
            else
                set.add(arr[i]);
        }
        return false;
    }
}
