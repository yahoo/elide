package graphql.java.generator.argument.strategies;

import java.util.List;

import graphql.java.generator.argument.ArgContainer;
import graphql.java.generator.strategies.Strategy;

public interface ArgumentObjectsStrategy extends Strategy {

    List<ArgContainer> getArgumentRepresentativeObjects(Object object);
    
}
