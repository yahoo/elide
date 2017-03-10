package graphql.java.generator.type;

import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLEnumType.newEnum;

import java.util.List;
import graphql.java.generator.type.strategies.TypeStrategies;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.TypeResolver;

/**
 * Given any object, decide how you wish the GraphQL type to be generated.
 * Not yet certified with arrays.
 * @author dwinsor
 *
 */
public class FullTypeGenerator extends TypeGenerator {
    public FullTypeGenerator(TypeStrategies strategies) {
        super(strategies);
    }
    
    protected GraphQLOutputType generateOutputType(Object object) {
        //An enum is a special case in both java and graphql,
        //and must be checked for while generating other kinds of types
        GraphQLEnumType enumType = generateEnumType(object);
        if (enumType != null) {
            return enumType;
        }
        
        List<GraphQLFieldDefinition> fields = getOutputFieldDefinitions(object);
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        
        String typeName = getGraphQLTypeNameOrIdentityCode(object);
        GraphQLObjectType.Builder builder = newObject()
                .name(typeName)
                .fields(fields)
                .description(getTypeDescription(object));
        
        GraphQLInterfaceType[] interfaces = getInterfaces(object);
        if (interfaces != null) {
            builder.withInterfaces(interfaces);
        }
        return builder.build();
    }
    
    protected GraphQLInterfaceType generateInterfaceType(Object object) {
        List<GraphQLFieldDefinition> fieldDefinitions = getOutputFieldDefinitions(object);
        if (fieldDefinitions == null || fieldDefinitions.isEmpty()) {
            return null;
        }
        String name = getGraphQLTypeNameOrIdentityCode(object);
        TypeResolver typeResolver = getTypeResolver(object);
        String description = getTypeDescription(object);
        if (name == null || fieldDefinitions == null || typeResolver == null) {
            return null;
        }
        GraphQLInterfaceType.Builder builder = GraphQLInterfaceType.newInterface()
                .description(description)
                .fields(fieldDefinitions)
                .name(name)
                .typeResolver(typeResolver);
        return builder.build();
    }
    
    protected GraphQLInputType generateInputType(Object object) {
        //An enum is a special case in both java and graphql,
        //and must be checked for while generating other kinds of types
        GraphQLEnumType enumType = generateEnumType(object);
        if (enumType != null) {
            return enumType;
        }
        
        List<GraphQLInputObjectField> fields = getInputFieldDefinitions(object);
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        String typeName = getGraphQLTypeNameOrIdentityCode(object);
        
        GraphQLInputObjectType.Builder builder = new GraphQLInputObjectType.Builder();
        builder.name(typeName);
        builder.fields(fields);
        builder.description(getTypeDescription(object));
        return builder.build();
    }

    protected GraphQLEnumType generateEnumType(Object object) {
        String typeName = getGraphQLTypeNameOrIdentityCode(object);

        List<GraphQLEnumValueDefinition> enumValues = getEnumValues(object);
        if (enumValues == null) {
            return null;
        }
        GraphQLEnumType.Builder builder = newEnum();
        builder.name(typeName);
        builder.description(getTypeDescription(object));
        for (GraphQLEnumValueDefinition value : enumValues) {
            builder.value(value.getName(), value.getValue(), value.getDescription());
        }
        return builder.build();
    }
}
