package graphql.java.generator.field;

import java.util.List;

import graphql.java.generator.field.strategies.FieldStrategies;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;

public interface IFieldsGenerator {
    
    FieldStrategies getStrategies();
    
    List<GraphQLFieldDefinition> getOutputFields(Object object);
    
    List<GraphQLInputObjectField> getInputFields(Object object);
    
}