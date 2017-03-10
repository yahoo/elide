package graphql.java.generator.datafetcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import graphql.schema.DataFetchingEnvironment;

public class MethodInvokingDataFetcher extends ArgumentAwareDataFetcher {
    
    private Method method;
    private Object source;

    public MethodInvokingDataFetcher(final Method method) {
        this.setMethod(method);
    }
    
    public MethodInvokingDataFetcher(final Method method, final Object[] argValues) {
        this.setMethod(method);
        this.setArgValues(argValues);
    }
    
    @Override
    public Object get(DataFetchingEnvironment environment) {
        Object source = getSource();
        if (source == null) source = environment.getSource();
        if (source == null) return null;
        if (getMethod() == null) return null;
        try {
            return getMethod().invoke(source, getArgValues());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }
}
