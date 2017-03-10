package graphql.java.generator.type.strategies;

import graphql.java.generator.strategies.Strategy;
import graphql.schema.GraphQLInterfaceType;

public interface InterfacesStrategy extends Strategy {

    /**
     * Should return all interfaces, including those from superclasses
     * and from superinterfaces.
     * @param object an object representing a java class, but not itself an interface.
     * @return
     */
    GraphQLInterfaceType[] getInterfaces(Object object);
}
