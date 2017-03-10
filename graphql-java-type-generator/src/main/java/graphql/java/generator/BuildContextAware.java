package graphql.java.generator;

public interface BuildContextAware {
    BuildContext getContext();
    void setContext(BuildContext context);
}
