package graphql.java.generator;

public class ClassWithMethodsWithArguments {
    public String getStringConcat(String one, String two) {
        return "" + one + two;
    }
    public int getNumber(int number) {
        return number;
    }
    public int getNumber2Args(int number, boolean ignored) {
        return number;
    }
}
