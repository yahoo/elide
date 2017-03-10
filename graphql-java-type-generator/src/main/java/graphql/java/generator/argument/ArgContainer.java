package graphql.java.generator.argument;

public class ArgContainer {
    private Object originatingObject;
    private Object representativeObject;
    private String suggestedName;
    private int index;
    
    public ArgContainer(Object originatingObject, Object representativeObject,
            String suggestedName, int index) {
        this.originatingObject = originatingObject;
        this.representativeObject = representativeObject;
        this.suggestedName = suggestedName;
        this.index = index;
    }
    /**
     * 
     * @return the object where this argument was found; for instance
     * a {@link java.lang.reflect.Method} with a parameter.
     */
    public Object getOriginatingObject() {
        return originatingObject;
    }
    /**
     * 
     * @return an object that represents the argument, for instance
     * the class of the type of a java parameter.
     */
    public Object getRepresentativeObject() {
        return representativeObject;
    }
    /**
     * A default or suggested name may already be known for this argument
     * @return null if no suggested name, a String otherwise
     */
    public String getSuggestedName() {
        return suggestedName;
    }
    /**
     * A zero-based index
     * @return the index, or -1 if no semantic value.
     */
    public int getIndex() {
        return index;
    }
}
