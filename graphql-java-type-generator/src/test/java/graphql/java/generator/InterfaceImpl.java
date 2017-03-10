package graphql.java.generator;

public class InterfaceImpl implements InterfaceChild {
    
    @Override
    public String getParent() {
        return "parent";
    }
    
    @Override
    public String getChild() {
        return "child";
    }
    
}
