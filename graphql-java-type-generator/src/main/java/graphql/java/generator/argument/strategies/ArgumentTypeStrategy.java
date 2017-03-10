package graphql.java.generator.argument.strategies;

import graphql.java.generator.argument.ArgContainer;
import graphql.java.generator.strategies.Strategy;
import graphql.schema.GraphQLInputType;

public interface ArgumentTypeStrategy extends Strategy {

    GraphQLInputType getArgumentType(ArgContainer container);
    
}
