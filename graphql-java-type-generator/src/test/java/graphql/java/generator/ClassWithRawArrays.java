package graphql.java.generator;

public class ClassWithRawArrays {
    public int[] ints = new int[]{0};
    public String[] strings = new String[] {
            "asdf",
            "2",
            "test"
    };
    public SimpleObject[] objects = new SimpleObject[] {
            new SimpleObject(),
            null
    };
    public String[] getStrings() {
        return strings;
    }
    public SimpleObject[] getObjects() {
        return objects;
    }
    public int[] getInts() {
        return ints;
    }
}
