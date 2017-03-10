package graphql.java.generator.field.reflect;

import graphql.java.generator.UnsharableBuildContextStorer;
import graphql.java.generator.datafetcher.ArgumentExtractingDataFetcher;
import graphql.java.generator.datafetcher.GraphQLInputAwareDataFetcher;
import graphql.java.generator.datafetcher.MethodInvokingDataFetcher;
import graphql.java.generator.field.strategies.FieldDataFetcherStrategy;
import graphql.schema.FieldDataFetcher;
import graphql.schema.GraphQLArgument;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For a given {@link java.lang.reflect.Field} or {@link java.lang.reflect.Method}
 * return a DataFetcher that obtains data through that.
 * @author dwinsor
 *
 */
public class FieldDataFetcher_Reflection
        extends UnsharableBuildContextStorer
        implements FieldDataFetcherStrategy {
    private static Logger logger = LoggerFactory.getLogger(
            FieldDataFetcher_Reflection.class);

    @Override
    public Object getFieldFetcher(Object object) {
        if (object instanceof Field) {
            return getFieldFetcherFromField((Field) object);
        }
        if (object instanceof Method) {
            return getFieldFetcherFromMethod((Method) object);
        }
        return null;
    }
    
    protected Object getFieldFetcherFromField(Field field) {
        Class<?> clazz = field.getDeclaringClass();
        Method getter = getGetterMethod(field, clazz);
        if (getter != null) {
            //This is commented out because it is redundant, but included
            //here for documentation, to be changed later
            //fieldBuilder.dataFetcher(new PropertyDataFetcher(field.getName()));
        }
        else {
            if (!Modifier.isPublic(field.getModifiers())) {
                logger.debug("field [{}] is not public and has no getter", field);
                return null;
            }
            logger.debug("Direct field access Class [{}], field [{}], type [{}]",
                    clazz, field.getName(), field.getType());
            return new FieldDataFetcher(field.getName());
        }
        return null;
    }
    
    protected Object getFieldFetcherFromMethod(Method method) {
        return getFieldFetcherFromMethod(method, null);
    }
    
    protected Object getFieldFetcherFromMethod(Method method, Object methodSource) {
        MethodInvokingDataFetcher methodInvoker = new MethodInvokingDataFetcher(method);
        methodInvoker.setSource(methodSource);
        List<GraphQLArgument> arguments = getContext()
                .getArgumentsGeneratorStrategy().getArguments(method);
        if (arguments == null || arguments.isEmpty()) {
            return methodInvoker;
        }
        return new GraphQLInputAwareDataFetcher(
                new ArgumentExtractingDataFetcher(methodInvoker), arguments);
    }
    
    /**
     * @param field
     * @param clazzContainingField
     * @return
     */
    protected Method getGetterMethod(Field field, Class<?> clazzContainingField) {
        Class<?> type = field.getType();
        String fieldName = field.getName();
        String prefix = (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(boolean.class))
                ? "is" : "get";
        String getterName = prefix + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Method getter = clazzContainingField.getMethod(getterName);
            return getter;
        }
        catch (NoSuchMethodException e) {
        }
        
        //TODO Be more permissive in the name, allowing getobject() or isboolean()
        //something like getterName = prefix + fieldName;
        //this requires updated DataFetchers, so will not be done yet.
        
        return null;
    }
}
