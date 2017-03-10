package graphql.java.generator.type.reflect;

import java.util.HashMap;
import java.util.Map;

import graphql.java.generator.UnsharableBuildContextStorer;
import graphql.java.generator.type.strategies.InterfacesStrategy;
import graphql.schema.GraphQLInterfaceType;

public class Interfaces_Reflection
        extends UnsharableBuildContextStorer
        implements InterfacesStrategy {
    
    @Override
    public GraphQLInterfaceType[] getInterfaces(Object object) {
        //TODO handle generics?
        if (object instanceof Class<?>) {
            if (((Class<?>) object).isInterface()) {
                return null;
            }
            
            Map<Class<?>, GraphQLInterfaceType> interfaces = new HashMap<Class<?>, GraphQLInterfaceType>();
            getInterfaces(interfaces, (Class<?>) object);
            return interfaces.values().toArray(new GraphQLInterfaceType[0]);
        }
        return null;
    }

    protected void getInterfaces(
            final Map<Class<?>, GraphQLInterfaceType> interfaceMap,
            final Class<?> clazz) {
        
        for (Class<?> intf : clazz.getInterfaces()) {        
            if (interfaceMap.containsKey(intf)) {
                continue;
            }
            GraphQLInterfaceType iType = getContext().getInterfaceType(intf);
            if (iType != null) {
                interfaceMap.put(intf, iType);
            }
            getInterfaces(interfaceMap, intf);
        }
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null && superClazz != Object.class) {
            getInterfaces(interfaceMap, superClazz);
        }
    }
}
