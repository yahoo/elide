package graphql.java.generator;

public class InterfaceImplSecondary
        extends InterfaceImpl
        implements InterfaceSecondary {
    
    @Override
    public String getParent() {
        return "parent2";
    }
    
    @Override
    public String getChild() {
        return "child2";
    }
    
    public String getSecondary() {
        return "secondary";
    }
    
}
