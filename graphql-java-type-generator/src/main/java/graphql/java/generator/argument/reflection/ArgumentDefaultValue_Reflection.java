package graphql.java.generator.argument.reflection;

import graphql.java.generator.argument.ArgContainer;
import graphql.java.generator.argument.strategies.ArgumentDefaultValueStrategy;

public class ArgumentDefaultValue_Reflection implements ArgumentDefaultValueStrategy {
    
    @Override
    public Object getArgumentDefaultValue(ArgContainer container) {
        if (container == null) return null;
        Object object = container.getRepresentativeObject();
        if (object instanceof Class<?>) {
            return getArgumentDefaultValue((Class<?>) object);
        }
        return null;
    }
    
    protected Object getArgumentDefaultValue(Class<?> clazz) {
        return null;
    }
}
