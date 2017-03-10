package graphql.java.generator.type;

import java.lang.reflect.ParameterizedType;
import graphql.introspection.Introspection.TypeKind;
import graphql.java.generator.BuildContext;
import graphql.java.generator.type.strategies.TypeStrategies;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

/**
 * Given any object, decide how you wish the GraphQL type to be generated.
 * Only handles wrapping, otherwise delegates to the next type generator.
 * Not yet certified with arrays.
 * @author dwinsor
 *
 */
public class WrappingTypeGenerator
        extends TypeGenerator
        implements ChainableTypeGenerator {
    
    private TypeGenerator nextTypeGen;
    
    public WrappingTypeGenerator(TypeGenerator nextTypeGen) {
        this(nextTypeGen, nextTypeGen.getStrategies());
    }
    
    public WrappingTypeGenerator(TypeGenerator nextTypeGen, TypeStrategies strategies) {
        super(strategies);
        setNextGen(nextTypeGen);
    }
    
    @Override
    public GraphQLType getParameterizedType(Object object,
            ParameterizedType genericType, TypeKind typeKind) {
        TypeSpecContainer originalObject = new TypeSpecContainer(
                object, genericType, typeKind);
        TypeSpecContainer interiorObject = getInteriorObjectToGenerate(originalObject);
        if (interiorObject == null) {
            return getType(object, genericType, typeKind);
        }
        
        //using getParameterizedType is intentional,
        //in case multiple wrappers are desired
        GraphQLType interiorType = getParameterizedType(
                interiorObject.getRepresentativeObject(),
                interiorObject.getGenericType(),
                interiorObject.getTypeKind());
        return wrapType(interiorType, originalObject);
    }
    
    @Override
    protected GraphQLOutputType generateOutputType(Object object) {
        return getNextGen().generateOutputType(object);
    }

    @Override
    protected GraphQLInterfaceType generateInterfaceType(Object object) {
        return getNextGen().generateInterfaceType(object);
    }

    @Override
    protected GraphQLInputType generateInputType(Object object) {
        return getNextGen().generateInputType(object);
    }

    @Override
    protected GraphQLEnumType generateEnumType(Object object) {
        return getNextGen().generateEnumType(object);
    }
    
    @Override
    public TypeGenerator getNextGen() {
        return nextTypeGen;
    }

    @Override
    public void setNextGen(TypeGenerator nextTypeGen) {
        this.nextTypeGen = nextTypeGen;
    }
    
    @Override
    public void setContext(BuildContext context) {
        super.setContext(context);
        getNextGen().setContext(context);
    }

    protected TypeSpecContainer getInteriorObjectToGenerate(TypeSpecContainer originalObject) {
        return getStrategies().getTypeWrapperStrategy().getInteriorObjectToGenerate(originalObject);
    }

    protected GraphQLType wrapType(GraphQLType interiorType, TypeSpecContainer originalObject) {
        return getStrategies().getTypeWrapperStrategy().wrapType(interiorType, originalObject);
    }
}
