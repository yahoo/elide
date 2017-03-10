package graphql.java.generator.type.resolver;

import graphql.java.generator.UnsharableBuildContextStorer;
import graphql.java.generator.type.strategies.TypeResolverStrategy;
import graphql.schema.TypeResolver;

/**
 * Reuses 1 {@linkplain graphql.java.generator.type.resolver.TypeResolver_BuildContextAware TypeResolver_BuildContextAware}
 * per {@linkplain graphql.java.generator.BuildContext BuildContext}
 * since this TypeResolverStrategy_Caching itself is tied to only 1 BuildContext.
 * @author dwinsor
 *
 */
public class TypeResolverStrategy_Caching
        extends UnsharableBuildContextStorer
        implements TypeResolverStrategy {
    
    TypeResolver_BuildContextAware typeResolver = null;

    @Override
    public TypeResolver getTypeResolver(Object object) {
        if (typeResolver == null) {
            typeResolver = new TypeResolver_BuildContextAware(getContext());
        }
        return typeResolver;
    }
}
