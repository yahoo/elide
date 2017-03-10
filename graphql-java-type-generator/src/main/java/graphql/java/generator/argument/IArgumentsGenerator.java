package graphql.java.generator.argument;

import java.util.List;

import graphql.java.generator.argument.strategies.ArgumentStrategies;
import graphql.schema.GraphQLArgument;


public interface IArgumentsGenerator {
    
    List<GraphQLArgument> getArguments(Object object);
    
    ArgumentStrategies getStrategies();
    
}