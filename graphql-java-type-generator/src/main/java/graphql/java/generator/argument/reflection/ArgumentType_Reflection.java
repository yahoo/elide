package graphql.java.generator.argument.reflection;

import java.lang.reflect.ParameterizedType;

import graphql.introspection.Introspection.TypeKind;
import graphql.java.generator.UnsharableBuildContextStorer;
import graphql.java.generator.argument.ArgContainer;
import graphql.java.generator.argument.strategies.ArgumentTypeStrategy;
import graphql.schema.GraphQLInputType;

/**
 * @author dwinsor
 *
 */
public class ArgumentType_Reflection
        extends UnsharableBuildContextStorer
        implements ArgumentTypeStrategy {
    
    @Override
    public GraphQLInputType getArgumentType(ArgContainer container) {
        if (container == null) return null;
        Object object = container.getRepresentativeObject();
        if (object == null) return null;
        
        if (!(object instanceof ParameterizedType)) {
            return getContext().getInputType(object);
        }
        return (GraphQLInputType) getContext().getParameterizedType(
                object,
                (ParameterizedType) object,
                TypeKind.INPUT_OBJECT);
    }
}
