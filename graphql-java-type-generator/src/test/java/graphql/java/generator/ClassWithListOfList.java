package graphql.java.generator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class ClassWithListOfList {
    public List<List<Integer>> listOfListOfInts = new ArrayList<List<Integer>>() {{
        add(new ArrayList<Integer>() {{
            add(0);
        }});
    }};
    public List<List<Integer>> getListOfListOfInts() {
        return listOfListOfInts;
    }
}
