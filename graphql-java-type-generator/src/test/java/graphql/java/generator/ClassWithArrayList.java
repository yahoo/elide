package graphql.java.generator;

import java.util.Arrays;
import java.util.List;

public class ClassWithArrayList {
    public List<Integer> integers = Arrays.asList(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);

    public List<Integer> getIntegers() {
        return integers;
    }
}