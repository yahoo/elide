package graphql.java.generator.type;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.java.generator.BuildContext;
import graphql.java.generator.BuildContextAware;
import graphql.introspection.Introspection.TypeKind;
import graphql.java.generator.UnsharableBuildContextStorer;
import graphql.java.generator.type.strategies.TypeStrategies;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.TypeResolver;

/**
 * Given any object, decide how you wish the GraphQL type to be generated.
 * This class defines the implementation contract, and further TypeGenerators
 * must be built off of this, using {@link #getType(Object, ParameterizedType, TypeKind)}
 * Use {@link WrappingTypeGenerator} to create types from {@code List<?>}s, otherwise
 * this generator will be happy to create an object based on List.class, which
 * is not what you want.
 * Not yet certified with arrays.
 * @author dwinsor
 *
 */
public abstract class TypeGenerator
        extends UnsharableBuildContextStorer
        implements ITypeGenerator, BuildContextAware {
    private static Logger logger = LoggerFactory.getLogger(TypeGenerator.class);
    
    private final TypeStrategies strategies;
    private final TypeRepository typeRepository;
    
    public TypeGenerator(TypeStrategies strategies) {
        this.strategies = strategies;
        this.typeRepository = strategies.getTypeRepository();
    }
    
    /**
     * 
     * @param object A representative "object" from which to construct
     * a {@link GraphQLOutputType}, the exact type of which is contextual
     * @return
     */
    @Override
    public final GraphQLOutputType getOutputType(Object object) {
        return (GraphQLOutputType) getParameterizedType(object, null, TypeKind.OBJECT);
    }
    
    /**
     * @param object A representative "object" from which to construct
     * a {@link GraphQLInterfaceType}, the exact type of which is contextual,
     * but MUST represent a java interface, NOT an object or class with an interface.
     * Will be stored internally as a {@link GraphQLOutputType}, so its name
     * must not clash with any GraphQLOutputType.
     * 
     * @return
     */
    @Override
    public final GraphQLInterfaceType getInterfaceType(Object object) {
        return (GraphQLInterfaceType) getParameterizedType(object, null, TypeKind.INTERFACE);
    }

    /**
     * @param object A representative "object" from which to construct
     * a {@link GraphQLInputType}, the exact type of which is contextual
     * @return
     */
    @Override
    public final GraphQLInputType getInputType(Object object) {
        return (GraphQLInputType) getParameterizedType(object, null, TypeKind.INPUT_OBJECT);
    }
    
    
    
    @Override
    public GraphQLType getParameterizedType(Object object,
            ParameterizedType genericType, TypeKind typeKind) {
        return getType(object, genericType, typeKind);
    }
    
    /**
     * An internal, unchanging impl.
     * @param object
     * @param genericType
     * @param typeKind
     * @return
     */
    protected final GraphQLType getType(Object object,
            ParameterizedType genericType, TypeKind typeKind) {
        logger.debug("{} object is [{}]", typeKind, object);
        
        //short circuit if it's a primitive type or some other user defined default
        GraphQLType defaultType = getDefaultType(object, typeKind);
        if (defaultType != null) {
            return defaultType;
        }
        
        
        String typeName = getGraphQLTypeName(object);
        if (typeName == null) {
            logger.debug("TypeName was null for object [{}]. "
                    + "Type will attempt to be built but not placed in the TypeRepository", object);
            return generateType(object, typeKind);
        }
        
        
        //this check must come before generated*Types.get
        //necessary for synchronicity to avoid duplicate object creations
        Set<String> typesBeingBuilt = getContext().getTypesBeingBuilt();
        if (typesBeingBuilt.contains(typeName)) {
            logger.debug("Using a reference to: [{}]", typeName);
            if (TypeKind.OBJECT.equals(typeKind)) {
                return new GraphQLTypeReference(typeName);
            }
            logger.error("While constructing type, using a reference to: [{}]", typeName);
            throw new RuntimeException("Cannot put type-cycles into input or interface types, "
                    + "there is no GraphQLTypeReference");
        }
        
        
        GraphQLType prevType = getTypeRepository().getGeneratedType(typeName, typeKind);
        if (prevType != null) {
            return prevType;
        }
        
        
        typesBeingBuilt.add(typeName);
        try {
            GraphQLType type = generateType(object, typeKind);
            if (getTypeRepository() != null && type != null) {
                getTypeRepository().registerType(typeName, type, typeKind);
            }
            return type;
        }
        catch (RuntimeException e) {
            logger.warn("Failed to generate type named {} with kind {}", typeName, typeKind);
            logger.debug("Failed to generate type, exception is ", e);
            throw e;
        }
        finally {
            typesBeingBuilt.remove(typeName);
        }
    }

    protected GraphQLType generateType(Object object, TypeKind typeKind) {
        switch (typeKind) {
        case OBJECT:
            return generateOutputType(object);
        case INTERFACE:
            return generateInterfaceType(object);
        case INPUT_OBJECT:
            return generateInputType(object);
        case ENUM:
            return generateEnumType(object);
        default:
            return null;
        }
    }
    
    
    protected abstract GraphQLOutputType generateOutputType(Object object);
    protected abstract GraphQLInterfaceType generateInterfaceType(Object object);
    protected abstract GraphQLInputType generateInputType(Object object);
    protected abstract GraphQLEnumType generateEnumType(Object object);
    
    
    protected List<GraphQLFieldDefinition> getOutputFieldDefinitions(Object object) {
        List<GraphQLFieldDefinition> definitions = 
                getContext().getFieldsGeneratorStrategy()
                        .getOutputFields(object);
        return definitions;
    }
    
    protected List<GraphQLInputObjectField> getInputFieldDefinitions(Object object) {
        List<GraphQLInputObjectField> definitions = 
                getContext().getFieldsGeneratorStrategy()
                        .getInputFields(object);
        return definitions;
    }
    
    protected GraphQLType getDefaultType(Object object, TypeKind typeKind) {
        return getStrategies().getDefaultTypeStrategy().getDefaultType(object, typeKind);
    }
    
    protected String getGraphQLTypeNameOrIdentityCode(Object object) {
        String typeName = getGraphQLTypeName(object);
        if (typeName == null) {
            typeName = "Object_" + String.valueOf(System.identityHashCode(object));
        }
        return typeName;
    }
    protected String getGraphQLTypeName(Object object) {
        return getStrategies().getTypeNameStrategy().getTypeName(object);
    }
    
    protected String getTypeDescription(Object object) {
        return getStrategies().getTypeDescriptionStrategy().getTypeDescription(object);
    }
    
    protected List<GraphQLEnumValueDefinition> getEnumValues(Object object) {
        return getStrategies().getEnumValuesStrategy().getEnumValueDefinitions(object);
    }
    
    protected GraphQLInterfaceType[] getInterfaces(Object object) {
        return getStrategies().getInterfacesStrategy().getInterfaces(object);
    }

    protected TypeResolver getTypeResolver(Object object) {
        return getStrategies().getTypeResolverStrategy().getTypeResolver(object);
    }

    
    @Override
    public void setContext(BuildContext context) {
        super.setContext(context);
        getStrategies().setContext(context);
    }

    @Override
    public TypeStrategies getStrategies() {
        return strategies;
    }

    protected TypeRepository getTypeRepository() {
        return typeRepository;
    }
}
