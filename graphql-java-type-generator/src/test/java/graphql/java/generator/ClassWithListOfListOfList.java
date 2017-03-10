package graphql.java.generator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class ClassWithListOfListOfList {
    public List<List<List<Integer>>> listOfListOfListOfInts = new ArrayList<List<List<Integer>>>() {{
        add(new ArrayList<List<Integer>>() {{
            add(new ArrayList<Integer>() {{
                add(0);
            }});
        }});
    }};
    public List<List<List<Integer>>> getListOfListOfListOfInts() {
        return listOfListOfListOfInts;
    }
}
