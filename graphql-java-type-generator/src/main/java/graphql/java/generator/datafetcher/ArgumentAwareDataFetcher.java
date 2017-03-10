package graphql.java.generator.datafetcher;

public abstract class ArgumentAwareDataFetcher implements ArgAwareDataFetcher {
    private static final Object[] emptyValues = new Object[0];
    private static final String[] emptyNames = new String[0];
    
    private Object[] argValues = emptyValues;
    private String[] argNames = emptyNames;

    @Override
    public Object[] getArgValues() {
        return argValues;
    }

    @Override
    public void setArgValues(Object[] argValues) {
        this.argValues = argValues;
    }

    @Override
    public String[] getArgNames() {
        return argNames;
    }

    @Override
    public void setArgNames(String[] argNames) {
        this.argNames = argNames;
    }
}
