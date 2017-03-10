package graphql.java.generator;

public class ParameterizedClass<T> {
    T t;
    public T getT() {
        return t;
    }
    public void setT(T t) {
        this.t= t;
    }
}
