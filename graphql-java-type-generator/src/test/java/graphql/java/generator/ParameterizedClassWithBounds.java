package graphql.java.generator;

public class ParameterizedClassWithBounds<T extends InterfaceChild> {
    T t;
    /**
     * Named this way to assist in testing / serializing.
     * @return
     */
    public T getInterfaceImpl() {
        return t;
    }
    public void setT(T t) {
        this.t= t;
    }
}
