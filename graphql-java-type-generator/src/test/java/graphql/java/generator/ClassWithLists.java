package graphql.java.generator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public class ClassWithLists {
    public List<Integer> ints = new ArrayList<Integer>() {{
        add(0);
    }};
//    public List noTypeInfo = new ArrayList<Integer>() {{
//        add(0);
//    }};
    public ArrayList<String> strings = new ArrayList<String>() {{
        add("asdf");
        add("2");
        add("test");
    }};
    public LinkedList<SimpleObject> objects = new LinkedList<SimpleObject>() {{
        add(new SimpleObject());
        add(null);
        SimpleObject obj = new SimpleObject();
        obj.objIndex = 1;
        add(new SimpleObject());
        add(obj);
    }};
    public ArrayList<String> getStrings() {
        return strings;
    }
    public List<SimpleObject> getObjects() {
        return objects;
    }
    public List<Integer> getInts() {
        return ints;
    }
}
