package graphql.java.generator.type.resolver;

import graphql.java.generator.BuildContext;
import graphql.java.generator.UnsharableBuildContextStorer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

public class TypeResolver_BuildContextAware
        extends UnsharableBuildContextStorer
        implements TypeResolver {

    public TypeResolver_BuildContextAware() {
    }
    
    public TypeResolver_BuildContextAware(final BuildContext context) {
        setContext(context);
    }
    
    @Override
    public GraphQLObjectType getType(Object object) {
        return (GraphQLObjectType) getContext().getOutputType(object);
    }
}
